package com.inputmutator.engine.model;

/**
 * Defines the resolution level at which mutations are applied to inputs.
 */
public enum GranularityMode {
    /**
     * Mutates tokens as entire cohesive units (default, compact).
     */
    TOKEN_ONLY,

    /**
     * Sliding window mutating one individual character position at a time.
     */
    SINGLE_POSITION,

    /**
     * Mutates combinations of multiple character positions bounded by budget limits.
     */
    COMBINATORIAL
}
