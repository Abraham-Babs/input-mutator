package com.inputmutator.engine.pipeline;

import com.inputmutator.engine.constraint.ConstraintProfile;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Stateful tracking session across transformation steps to eliminate cycles and redundant encodings.
 */
public class TransformationContext {

    private final ConstraintProfile profile;
    private final Set<String> visitedStates;

    public TransformationContext(ConstraintProfile profile) {
        this(profile, new HashSet<>());
    }

    public TransformationContext(ConstraintProfile profile, Set<String> visitedStates) {
        this.profile = Objects.requireNonNull(profile, "profile must not be null");
        this.visitedStates = Objects.requireNonNull(visitedStates, "visitedStates must not be null");
    }

    public ConstraintProfile profile() {
        return profile;
    }

    /**
     * Attempts to register the candidate string.
     * @return true if state was newly added; false if previously visited (cycle/duplicate).
     */
    public boolean markVisited(String state) {
        if (state == null) {
            return false;
        }
        return visitedStates.add(state);
    }
}
