package com.inputmutator.engine;

import com.inputmutator.engine.constraint.ConstraintProfile;
import com.inputmutator.engine.constraint.ConstraintValidator;
import com.inputmutator.engine.model.InputType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConstraintValidatorTest {

    private ConstraintValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ConstraintValidator();
    }

    @Test
    void testLengthLimits() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .minLength(3)
                .maxLength(8)
                .build();

        assertFalse(validator.isValid("ab", profile));
        assertTrue(validator.isValid("abc", profile));
        assertTrue(validator.isValid("12345678", profile));
        assertFalse(validator.isValid("123456789", profile));
    }

    @Test
    void testNullByteFiltering() {
        ConstraintProfile denyNulls = ConstraintProfile.builder()
                .allowNullBytes(false)
                .build();
        ConstraintProfile allowNulls = ConstraintProfile.builder()
                .allowNullBytes(true)
                .allowNonPrintable(true)
                .build();

        assertFalse(validator.isValid("admin\0root", denyNulls));
        assertTrue(validator.isValid("admin\0root", allowNulls));
    }

    @Test
    void testControlCharFiltering() {
        ConstraintProfile denyControl = ConstraintProfile.builder()
                .allowNonPrintable(false)
                .build();
        ConstraintProfile allowControl = ConstraintProfile.builder()
                .allowNonPrintable(true)
                .build();

        assertFalse(validator.isValid("test\u0007value", denyControl));
        assertTrue(validator.isValid("test\u0007value", allowControl));
    }

    @Test
    void testPreserveEmailStructure() {
        ConstraintProfile emailProfile = ConstraintProfile.builder()
                .inputType(InputType.EMAIL)
                .preserveStructure(true)
                .build();

        assertTrue(validator.isValid("user@domain.com", emailProfile));
        assertTrue(validator.isValid("\"user@escaped\"@domain.com", emailProfile));
        assertFalse(validator.isValid("userdomain.com", emailProfile));
        assertFalse(validator.isValid("@domain.com", emailProfile));
        assertFalse(validator.isValid("user@@domain.com", emailProfile));
    }

    @Test
    void testPreserveJsonStructure() {
        ConstraintProfile jsonProfile = ConstraintProfile.builder()
                .inputType(InputType.JSON)
                .preserveStructure(true)
                .build();

        assertTrue(validator.isValid("{\"key\": \"val\"}", jsonProfile));
        assertTrue(validator.isValid("[1, 2, 3]", jsonProfile));
        assertFalse(validator.isValid("{\"key\": \"val\"", jsonProfile));
        assertFalse(validator.isValid("{\"key\": \"val\"]", jsonProfile)); // mismatched brace/bracket
        assertFalse(validator.isValid("{\"key\": \"val", jsonProfile)); // unclosed string
    }

    @Test
    void testPreserveNumericStructure() {
        ConstraintProfile numProfile = ConstraintProfile.builder()
                .inputType(InputType.NUMERIC)
                .preserveStructure(true)
                .build();

        assertTrue(validator.isValid("42", numProfile));
        assertTrue(validator.isValid("-0.0", numProfile));
        assertTrue(validator.isValid(".5", numProfile));
        assertTrue(validator.isValid("NaN", numProfile));
        assertTrue(validator.isValid("Infinity", numProfile));
        assertTrue(validator.isValid("-Infinity", numProfile));
        assertTrue(validator.isValid("0x1A", numProfile));
        assertTrue(validator.isValid("1e-308", numProfile));
        assertFalse(validator.isValid("42a", numProfile));
        assertFalse(validator.isValid("abc", numProfile));
    }
}
