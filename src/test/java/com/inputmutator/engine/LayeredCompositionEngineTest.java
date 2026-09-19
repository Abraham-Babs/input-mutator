package com.inputmutator.engine;

import com.inputmutator.engine.constraint.ConstraintProfile;
import com.inputmutator.engine.pipeline.LayeredCompositionEngine;
import com.inputmutator.engine.pipeline.Mutator;
import com.inputmutator.engine.pipeline.TransformationContext;
import com.inputmutator.engine.pipeline.mutators.EscapingMutator;
import com.inputmutator.engine.pipeline.mutators.UnicodeMutator;
import com.inputmutator.engine.tokenizer.Token;
import com.inputmutator.engine.tokenizer.TokenType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LayeredCompositionEngineTest {

    @Test
    void testLayeringDisabledWhenLayersUnderTwo() {
        LayeredCompositionEngine engine = new LayeredCompositionEngine();
        ConstraintProfile profile = ConstraintProfile.builder()
                .encodingLayers(1)
                .build();
        TransformationContext context = new TransformationContext(profile);
        Token token = new Token(TokenType.LITERAL, "test", 0, 4);

        List<String> results = engine.composeLayers(token, context, List.of(new UnicodeMutator(), new EscapingMutator()));
        assertTrue(results.isEmpty());
    }

    @Test
    void testLayeringComposesCharacterMutationWithWireEncoding() {
        LayeredCompositionEngine engine = new LayeredCompositionEngine();
        ConstraintProfile profile = ConstraintProfile.builder()
                .encodingLayers(2)
                .build();
        TransformationContext context = new TransformationContext(profile);
        Token token = new Token(TokenType.LITERAL, "admin", 0, 5);

        List<Mutator> mutators = List.of(new UnicodeMutator(), new EscapingMutator());
        List<String> results = engine.composeLayers(token, context, mutators);

        assertNotNull(results);
        assertFalse(results.isEmpty());
        // Verify that composite permutations contain percent encodings of mutated characters
        assertTrue(results.stream().anyMatch(s -> s.contains("%")));
    }
}
