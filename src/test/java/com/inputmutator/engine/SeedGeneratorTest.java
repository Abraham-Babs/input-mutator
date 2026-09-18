package com.inputmutator.engine;

import com.inputmutator.engine.constraint.ConstraintProfile;
import com.inputmutator.engine.model.InputType;
import com.inputmutator.engine.pipeline.SeedGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SeedGeneratorTest {

    private SeedGenerator seedGenerator;

    @BeforeEach
    void setUp() {
        seedGenerator = new SeedGenerator();
    }

    @Test
    void testNumericSeeds() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .inputType(InputType.NUMERIC)
                .build();
        List<String> seeds = seedGenerator.generateSeeds(profile);

        assertFalse(seeds.isEmpty());
        assertTrue(seeds.contains("0"));
        assertTrue(seeds.contains("-1"));
        assertTrue(seeds.contains("2147483647"));
        assertTrue(seeds.contains("NaN"));
    }

    @Test
    void testEmailSeeds() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .inputType(InputType.EMAIL)
                .build();
        List<String> seeds = seedGenerator.generateSeeds(profile);

        assertFalse(seeds.isEmpty());
        assertTrue(seeds.stream().allMatch(s -> s.contains("@")));
        assertTrue(seeds.contains("\"user name\"@example.com"));
    }

    @Test
    void testJsonSeeds() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .inputType(InputType.JSON)
                .build();
        List<String> seeds = seedGenerator.generateSeeds(profile);

        assertFalse(seeds.isEmpty());
        assertTrue(seeds.contains("{}"));
        assertTrue(seeds.contains("[]"));
    }

    @Test
    void testUrlPathSeeds() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .inputType(InputType.URL_PATH)
                .build();
        List<String> seeds = seedGenerator.generateSeeds(profile);

        assertFalse(seeds.isEmpty());
        assertTrue(seeds.contains("/"));
        assertTrue(seeds.contains("/../"));
    }
}
