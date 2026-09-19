package com.inputmutator.engine;

import com.inputmutator.engine.constraint.ConstraintProfile;
import com.inputmutator.engine.constraint.ConstraintValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ControlCharValidationTest {

    private ConstraintValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ConstraintValidator();
    }

    @Test
    void testC0AndC1ControlCharactersFiltered() {
        ConstraintProfile denyControls = ConstraintProfile.builder()
                .allowNonPrintable(false)
                .build();
        ConstraintProfile allowControls = ConstraintProfile.builder()
                .allowNonPrintable(true)
                .build();

        // C0 control BEL (0x07)
        assertFalse(validator.isValid("text\u0007end", denyControls));
        assertTrue(validator.isValid("text\u0007end", allowControls));

        // C1 control PAD (0x80) and APC (0x9F)
        assertFalse(validator.isValid("text\u0080end", denyControls));
        assertTrue(validator.isValid("text\u0080end", allowControls));
        assertFalse(validator.isValid("text\u009Fend", denyControls));
        assertTrue(validator.isValid("text\u009Fend", allowControls));

        // Lone surrogate (0xD800)
        assertFalse(validator.isValid("text\uD800end", denyControls));
        assertTrue(validator.isValid("text\uD800end", allowControls));
    }
}
