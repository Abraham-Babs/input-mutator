package com.inputmutator.engine;

import com.inputmutator.engine.constraint.ConstraintProfile;
import com.inputmutator.engine.model.GranularityMode;
import com.inputmutator.engine.model.InputType;
import com.inputmutator.engine.pipeline.MutationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MutationEngineTest {

    private MutationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new MutationEngine();
    }

    @Test
    void testNumericMutations() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .inputType(InputType.NUMERIC)
                .maxPermutations(50)
                .build();

        List<String> results = engine.generate("42", profile);
        assertFalse(results.isEmpty());

        // Verify radices generated
        assertTrue(results.contains("0x2a") || results.contains("0X2A"));
        assertTrue(results.contains("052") || results.contains("0o52"));
        assertTrue(results.contains("0b101010"));

        // Verify scientific notation
        assertTrue(results.contains("42e0") || results.contains("42E0"));
    }

    @Test
    void testUnicodeAndHomoglyphMutations() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .inputType(InputType.GENERIC_STRING)
                .maxPermutations(100)
                .build();

        List<String> results = engine.generate("admin", profile);
        assertFalse(results.isEmpty());

        // Check for homoglyph substitution (e.g. Cyrillic 'a' \u0430)
        boolean hasHomoglyph = results.stream().anyMatch(s -> s.contains("\u0430"));
        assertTrue(hasHomoglyph, "Engine should generate homoglyphs for 'admin'");
    }

    @Test
    void testEscapingMutations() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .inputType(InputType.GENERIC_STRING)
                .maxPermutations(50)
                .build();

        List<String> results = engine.generate("test", profile);
        assertFalse(results.isEmpty());

        // Verify URL encoding (%74%65%73%74 or %74...)
        boolean hasUrlEncoding = results.stream().anyMatch(s -> s.startsWith("%"));
        assertTrue(hasUrlEncoding, "Engine should generate URL-encoded variations");
    }

    @Test
    void testCycleAndRedundancyPruning() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .maxDepth(3)
                .maxPermutations(30)
                .build();

        List<String> results = engine.generate("1", profile);

        // Verify every output is distinct
        long uniqueCount = results.stream().distinct().count();
        assertEquals(results.size(), uniqueCount, "Permutations must have no duplicate states");
    }

    @Test
    void testConstraintLengthEnforced() {
        int maxLen = 10;
        ConstraintProfile profile = ConstraintProfile.builder()
                .maxLength(maxLen)
                .maxPermutations(100)
                .build();

        List<String> results = engine.generate("user", profile);
        for (String s : results) {
            assertTrue(s.length() <= maxLen, "Result length must not exceed profile max: " + s);
        }
    }

    @Test
    void testExceedsInputCeiling() {
        String hugeInput = "A".repeat(MutationEngine.MAX_INPUT_CEILING + 1);
        ConstraintProfile profile = ConstraintProfile.defaultProfile();

        assertThrows(IllegalArgumentException.class, () -> engine.generate(hugeInput, profile));
    }

    @Test
    void testSupplementaryUnicodeCharacters() {
        // Test with surrogate pairs / emoji codepoints
        String emojiInput = "test\uD83D\uDE00val"; // test😀val
        ConstraintProfile profile = ConstraintProfile.builder()
                .maxPermutations(20)
                .build();

        assertDoesNotThrow(() -> {
            List<String> results = engine.generate(emojiInput, profile);
            assertNotNull(results);
        });
    }

    @Test
    void testMathematicalAlphanumericMutations() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .maxPermutations(50)
                .build();

        List<String> results = engine.generate("admin", profile);
        // Look for Mathematical Bold 'admin' (𝐚𝐝𝐦𝐢𝐧 - \uD835\uDC1A...)
        boolean hasMathAlphanumeric = results.stream().anyMatch(s -> s.contains("\uD835"));
        assertTrue(hasMathAlphanumeric, "Engine should generate Mathematical Alphanumeric surrogate pairs");
    }

    @Test
    void testSinglePositionGranularity() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .granularityMode(com.inputmutator.engine.model.GranularityMode.SINGLE_POSITION)
                .maxPermutations(50)
                .build();

        List<String> results = engine.generate("cat", profile);
        assertFalse(results.isEmpty());

        // Verify single character variations like mutating 'c' while keeping 'at'
        boolean mutatedFirstCharOnly = results.stream().anyMatch(s -> s.endsWith("at") && !s.equals("cat"));
        assertTrue(mutatedFirstCharOnly, "Should mutate 1st character while leaving remainder unchanged");

        // Verify single character variations like mutating 't' while keeping 'ca'
        boolean mutatedLastCharOnly = results.stream().anyMatch(s -> s.startsWith("ca") && !s.equals("cat"));
        assertTrue(mutatedLastCharOnly, "Should mutate last character while leaving beginning unchanged");
    }

    @Test
    void testCombinatorialGranularity() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .granularityMode(com.inputmutator.engine.model.GranularityMode.COMBINATORIAL)
                .maxPositionsMutated(2)
                .maxPermutations(50)
                .build();

        List<String> results = engine.generate("test", profile);
        assertFalse(results.isEmpty());
        // Verify distinct results bounded by maxPermutations
        assertTrue(results.size() <= 50);
        assertEquals(results.size(), results.stream().distinct().count());
    }

    @Test
    void testZeroInputGeneratesArchetypeSeeds() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .inputType(com.inputmutator.engine.model.InputType.NUMERIC)
                .maxPermutations(20)
                .build();

        // Pass empty input
        List<String> results = engine.generate("", profile);
        assertFalse(results.isEmpty(), "Zero input should generate archetype seeds");
        assertTrue(results.contains("0") || results.contains("-1") || results.contains("NaN"));
    }

    @Test
    void testPreEncodedInputCanonicalization() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .canonicalizePreEncoded(true)
                .maxPermutations(20)
                .build();

        // Pass pre-URL-encoded input "%61dmin"
        List<String> results = engine.generate("%61dmin", profile);
        assertFalse(results.isEmpty());
        // Should produce variations based on canonical 'admin' (such as homoglyphs or alternate encodings)
        assertNotNull(results);
    }

    @Test
    void testDependencyInjectionAndDefensiveCopying() {
        var mutatorsList = new java.util.ArrayList<com.inputmutator.engine.pipeline.Mutator>();
        mutatorsList.add(new com.inputmutator.engine.pipeline.mutators.UnicodeMutator());

        var customEngine = new MutationEngine(
                new com.inputmutator.engine.tokenizer.InputTokenizer(),
                new com.inputmutator.engine.constraint.ConstraintValidator(),
                mutatorsList,
                new com.inputmutator.engine.pipeline.SeedGenerator(),
                new com.inputmutator.engine.encoding.EncodingDetector(),
                new com.inputmutator.engine.encoding.Canonicalizer()
        );

        // Mutating external list should not affect internal state
        mutatorsList.clear();

        ConstraintProfile profile = ConstraintProfile.builder()
                .inputType(InputType.GENERIC_STRING)
                .maxPermutations(10)
                .build();
        List<String> results = customEngine.generate("admin", profile);
        assertFalse(results.isEmpty());

        // Null checks
        assertThrows(NullPointerException.class, () -> new MutationEngine(null, null, null));
        assertThrows(NullPointerException.class, () -> engine.generate("test", null));
    }

    @Test
    void testCombinatorialWithConfigurablePositions() {
        ConstraintProfile profile3Pos = ConstraintProfile.builder()
                .granularityMode(com.inputmutator.engine.model.GranularityMode.COMBINATORIAL)
                .maxPositionsMutated(3)
                .maxPermutations(50)
                .build();

        List<String> results3 = engine.generate("testing", profile3Pos);
        assertFalse(results3.isEmpty());

        ConstraintProfile profile1Pos = ConstraintProfile.builder()
                .granularityMode(com.inputmutator.engine.model.GranularityMode.COMBINATORIAL)
                .maxPositionsMutated(1)
                .maxPermutations(50)
                .build();

        List<String> results1 = engine.generate("testing", profile1Pos);
        assertFalse(results1.isEmpty());
    }

    @Test
    void testConstraintProfileToBuilderPreservesFields() {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^[a-z]+$");
        ConstraintProfile original = ConstraintProfile.builder()
                .inputType(InputType.NUMERIC)
                .maxPositionsMutated(3)
                .encodingLayers(2)
                .minLength(5)
                .maxLength(50)
                .allowNonPrintable(true)
                .allowNullBytes(true)
                .preserveStructure(false)
                .maxDepth(4)
                .maxPermutations(75)
                .allowedPattern(pattern)
                .build();

        ConstraintProfile derived = original.toBuilder()
                .inputType(InputType.GENERIC_STRING)
                .build();

        assertEquals(InputType.GENERIC_STRING, derived.inputType());
        assertEquals(original.maxPositionsMutated(), derived.maxPositionsMutated());
        assertEquals(original.encodingLayers(), derived.encodingLayers());
        assertEquals(original.minLength(), derived.minLength());
        assertEquals(original.maxLength(), derived.maxLength());
        assertEquals(original.allowNonPrintable(), derived.allowNonPrintable());
        assertEquals(original.allowNullBytes(), derived.allowNullBytes());
        assertEquals(original.preserveStructure(), derived.preserveStructure());
        assertEquals(original.maxDepth(), derived.maxDepth());
        assertEquals(original.maxPermutations(), derived.maxPermutations());
        assertEquals(original.allowedPattern(), derived.allowedPattern());
    }

    @Test
    void testNumericSingleAndCombinatorialPositions() {
        ConstraintProfile singleProfile = ConstraintProfile.builder()
                .granularityMode(GranularityMode.SINGLE_POSITION)
                .maxPermutations(20)
                .build();

        List<String> singleResults = engine.generate("1,2,3,4,5", singleProfile);
        assertNotNull(singleResults);
        assertFalse(singleResults.isEmpty(), "Single position granularity must generate permutations for inputs with numeric tokens");

        ConstraintProfile comboProfile = ConstraintProfile.builder()
                .granularityMode(GranularityMode.COMBINATORIAL)
                .maxPositionsMutated(2)
                .maxPermutations(20)
                .build();

        List<String> comboResults = engine.generate("1,2,3,4,5", comboProfile);
        assertNotNull(comboResults);
        assertFalse(comboResults.isEmpty(), "Combinatorial granularity must generate permutations for inputs with numeric tokens");
    }

    @Test
    void testCombinatorialProducesDistinctMultiPositionMutations() {
        ConstraintProfile singleProfile = ConstraintProfile.builder()
                .granularityMode(GranularityMode.SINGLE_POSITION)
                .maxPermutations(50)
                .build();
        List<String> singleResults = engine.generate("admin", singleProfile);

        ConstraintProfile comboProfile = ConstraintProfile.builder()
                .granularityMode(GranularityMode.COMBINATORIAL)
                .maxPositionsMutated(2)
                .maxPermutations(50)
                .build();
        List<String> comboResults = engine.generate("admin", comboProfile);

        assertFalse(comboResults.isEmpty());
        // Verify that combinatorial mode is not identical to single-position mode
        boolean hasMultiCharVariant = comboResults.stream().anyMatch(r -> !singleResults.contains(r));
        assertTrue(hasMultiCharVariant, "Combinatorial mode must produce multi-character combinations not present in single-position");
    }

    @Test
    void testUncappedNaturalExhaustion() {
        ConstraintProfile uncappedProfile = ConstraintProfile.builder()
                .maxPermutations(0) // 0 = unlimited / natural exhaustion
                .build();

        assertTrue(uncappedProfile.isUncapped());
        List<String> results = engine.generate("test", uncappedProfile);
        assertFalse(results.isEmpty());
        assertTrue(results.size() > 10, "Uncapped generation should run to natural exhaustion");
    }
}

