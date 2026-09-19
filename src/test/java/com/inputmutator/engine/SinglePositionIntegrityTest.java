package com.inputmutator.engine;

import com.inputmutator.engine.constraint.ConstraintProfile;
import com.inputmutator.engine.model.GranularityMode;
import com.inputmutator.engine.model.InputType;
import com.inputmutator.engine.pipeline.MutationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SinglePositionIntegrityTest {

    private MutationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new MutationEngine();
    }

    @Test
    void testSinglePositionDoesNotInjectEmailConstructsMidWord() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .inputType(InputType.EMAIL)
                .granularityMode(GranularityMode.SINGLE_POSITION)
                .maxPermutations(50)
                .build();

        List<String> results = engine.generate("test@example.com", profile);
        assertNotNull(results);

        // No result should contain local constructs embedded inside mid-character positions
        for (String perm : results) {
            assertFalse(perm.contains("(test)"), "Single position should not splice comments mid-character: " + perm);
            assertFalse(perm.contains("+tag"), "Single position should not splice email tags mid-character: " + perm);
        }
    }
}
