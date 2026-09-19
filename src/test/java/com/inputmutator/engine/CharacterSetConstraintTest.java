package com.inputmutator.engine;

import com.inputmutator.engine.constraint.CharacterSetConstraint;
import com.inputmutator.engine.constraint.ConstraintProfile;
import com.inputmutator.engine.constraint.ConstraintValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CharacterSetConstraintTest {

    private ConstraintValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ConstraintValidator();
    }

    @Test
    void testAnyCharacterSetConstraint() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .characterSetConstraint(CharacterSetConstraint.ANY)
                .build();

        assertTrue(validator.isValid("admin", profile));
        assertTrue(validator.isValid("adm\u0456n", profile)); // Cyrillic i
        assertTrue(validator.isValid("admin%20root", profile));
    }

    @Test
    void testAsciiOnlyConstraint() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .characterSetConstraint(CharacterSetConstraint.ASCII_ONLY)
                .build();

        assertTrue(validator.isValid("admin", profile));
        assertTrue(validator.isValid("test_123!#", profile));
        assertFalse(validator.isValid("adm\u0456n", profile)); // Cyrillic i rejected
    }

    @Test
    void testAlphanumericConstraint() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .characterSetConstraint(CharacterSetConstraint.ALPHANUMERIC)
                .build();

        assertTrue(validator.isValid("admin123", profile));
        assertFalse(validator.isValid("admin_123", profile)); // Underscore rejected
        assertFalse(validator.isValid("adm\u0456n", profile)); // Non-ASCII rejected
        assertFalse(validator.isValid("admin%20", profile));  // Symbol rejected
    }

    @Test
    void testPrintableAsciiConstraint() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .characterSetConstraint(CharacterSetConstraint.PRINTABLE_ASCII)
                .build();

        assertTrue(validator.isValid("hello world!", profile));
        assertFalse(validator.isValid("hello\tworld", profile)); // Tab is 0x09 (< 0x20)
        assertFalse(validator.isValid("hello\u0000world", profile));
    }
}
