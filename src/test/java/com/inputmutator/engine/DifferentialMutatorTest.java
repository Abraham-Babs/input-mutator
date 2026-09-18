package com.inputmutator.engine;

import com.inputmutator.engine.constraint.ConstraintProfile;
import com.inputmutator.engine.model.InputType;
import com.inputmutator.engine.pipeline.MutationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DifferentialMutatorTest {

    private MutationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new MutationEngine();
    }

    @Test
    void testUrlDifferentialMutations() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .inputType(InputType.URL_PATH)
                .maxPermutations(50)
                .build();

        List<String> results = engine.generate("/admin", profile);
        assertFalse(results.isEmpty());

        // Verify path matrix parameter injection
        boolean hasMatrix = results.stream().anyMatch(s -> s.contains(";"));
        assertTrue(hasMatrix, "Should generate path matrix parameters e.g. ;param=1");

        // Verify dot-segment normalization variations
        boolean hasDotSegments = results.stream().anyMatch(s -> s.contains("/.") || s.contains("/.."));
        assertTrue(hasDotSegments, "Should generate dot-segment traversal variations");
    }

    @Test
    void testEmailDifferentialMutations() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .inputType(InputType.EMAIL)
                .maxPermutations(50)
                .build();

        List<String> results = engine.generate("admin@target.com", profile);
        assertFalse(results.isEmpty());

        // Verify sub-addressing
        boolean hasSubAddressing = results.stream().anyMatch(s -> s.contains("+"));
        assertTrue(hasSubAddressing, "Should generate sub-addressing (+tag) in email local part");

        // Verify quoted local-part or comments
        boolean hasRfcQuirks = results.stream().anyMatch(s -> s.contains("\"") || s.contains("("));
        assertTrue(hasRfcQuirks, "Should generate RFC 5322 comment or quoted-string local-parts");
    }

    @Test
    void testJsonDifferentialMutations() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .inputType(InputType.JSON)
                .maxPermutations(50)
                .build();

        List<String> results = engine.generate("{\"admin\":true}", profile);
        assertFalse(results.isEmpty());

        // Verify Unicode-escaped key
        boolean hasUnicodeEscapedKey = results.stream().anyMatch(s -> s.contains("\\u0061") || s.contains("\\u"));
        assertTrue(hasUnicodeEscapedKey, "Should generate Unicode-escaped JSON keys");
    }

    @Test
    void testNumericCulturalAndFullwidthDigits() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .inputType(InputType.NUMERIC)
                .maxPermutations(50)
                .build();

        List<String> results = engine.generate("42", profile);
        assertFalse(results.isEmpty());

        // Verify fullwidth digits ４２ (\uFF14\uFF12)
        boolean hasFullwidth = results.stream().anyMatch(s -> s.contains("\uFF14") || s.contains("\uFF12"));
        assertTrue(hasFullwidth, "Should generate fullwidth digits for numeric inputs");
    }

    @Test
    void testUnicodeNfkcAndCollationSplitters() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .inputType(InputType.GENERIC_STRING)
                .maxPermutations(50)
                .build();

        List<String> results = engine.generate("admin", profile);
        assertFalse(results.isEmpty());

        // Verify soft-hyphen or zero-width splitters
        boolean hasSplitter = results.stream().anyMatch(s -> s.contains("\u00AD") || s.contains("\u200C") || s.contains("\u200B"));
        assertTrue(hasSplitter, "Should generate soft-hyphen or zero-width splitters in string tokens");
    }

    @Test
    void testGreekAndTurkishConfusables() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .inputType(InputType.GENERIC_STRING)
                .maxPermutations(80)
                .build();

        List<String> results = engine.generate("admin", profile);
        assertFalse(results.isEmpty());

        // Verify Greek alpha (\u03B1) or Turkish dotless i (\u0131)
        boolean hasConfusable = results.stream().anyMatch(s -> s.contains("\u03B1") || s.contains("\u0131"));
        assertTrue(hasConfusable, "Should generate Greek or Turkish confusables");
    }

    @Test
    void testCircledAlphanumerics() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .inputType(InputType.GENERIC_STRING)
                .maxPermutations(80)
                .build();

        List<String> results = engine.generate("admin", profile);
        assertFalse(results.isEmpty());

        // Verify circled 'a' (\u24D0)
        boolean hasCircled = results.stream().anyMatch(s -> s.contains("\u24D0"));
        assertTrue(hasCircled, "Should generate circled / enclosed alphanumerics");
    }

    @Test
    void testOverlongHtmlEntityAndOctalEscapes() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .inputType(InputType.GENERIC_STRING)
                .maxPermutations(80)
                .build();

        List<String> results = engine.generate("admin", profile);
        assertFalse(results.isEmpty());

        // Verify 8-digit overlong zero-padded entity or octal escape
        boolean hasOverlongOrOctal = results.stream().anyMatch(s -> s.contains("&#00000097;") || s.contains("\\141"));
        assertTrue(hasOverlongOrOctal, "Should generate 8-digit overlong zero-padded HTML entities or octal escapes");
    }

    @Test
    void testFractionSlashVariants() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .inputType(InputType.URL_PATH)
                .maxPermutations(80)
                .build();

        List<String> results = engine.generate("/admin", profile);
        assertFalse(results.isEmpty());

        // Verify Unicode fraction slash (\u2044) or division slash (\u2215)
        boolean hasSlashVariant = results.stream().anyMatch(s -> s.contains("\u2044") || s.contains("\u2215"));
        assertTrue(hasSlashVariant, "Should generate Unicode fraction or division slash variants");
    }
}
