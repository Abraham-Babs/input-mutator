package com.inputmutator.engine.pipeline;

import com.inputmutator.engine.tokenizer.Token;

import java.util.List;

/**
 * Strategy interface for token and value mutations.
 */
public interface Mutator {

    /**
     * Unique identifier for mutation tracking and lineage.
     */
    String name();

    /**
     * Category for strategy selection and intent-based filtering.
     */
    default com.inputmutator.engine.model.ArtifactCategory category() {
        return com.inputmutator.engine.model.ArtifactCategory.GRAMMAR_DIFFERENTIAL;
    }

    /**
     * Indicates whether this mutator can meaningfully target individual character slices in single-position mode.
     */
    default boolean supportsCharacterLevel() {
        return true;
    }

    /**
     * Determines whether this mutator can process the given token under the current context.
     */
    boolean appliesTo(Token token, TransformationContext context);

    /**
     * Produces alternative candidate representations for the target token.
     */
    List<String> mutate(Token token, TransformationContext context);
}
