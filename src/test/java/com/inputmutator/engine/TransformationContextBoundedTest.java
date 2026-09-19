package com.inputmutator.engine;

import com.inputmutator.engine.constraint.ConstraintProfile;
import com.inputmutator.engine.pipeline.TransformationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransformationContextBoundedTest {

    @Test
    void testCapacityEnforced() {
        ConstraintProfile profile = ConstraintProfile.defaultProfile();
        int capacity = 5;
        TransformationContext context = new TransformationContext(profile, new java.util.LinkedHashSet<>(), capacity);

        for (int i = 0; i < 10; i++) {
            assertTrue(context.markVisited("item_" + i));
        }

        // Newly added items should not be duplicatable
        assertFalse(context.markVisited("item_9"));

        // Old evicted item ("item_0") can be added again without error since it was pruned
        assertTrue(context.markVisited("item_0"));
    }
}
