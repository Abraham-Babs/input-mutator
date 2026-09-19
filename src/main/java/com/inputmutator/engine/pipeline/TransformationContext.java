package com.inputmutator.engine.pipeline;

import com.inputmutator.engine.constraint.ConstraintProfile;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Stateful tracking session across transformation steps to eliminate cycles and redundant encodings.
 */
public class TransformationContext {

    public static final int DEFAULT_MAX_VISITED_STATES = 50_000;

    private final ConstraintProfile profile;
    private final Set<String> visitedStates;
    private final int maxCapacity;

    public TransformationContext(ConstraintProfile profile) {
        this(profile, new java.util.LinkedHashSet<>(), DEFAULT_MAX_VISITED_STATES);
    }

    public TransformationContext(ConstraintProfile profile, Set<String> visitedStates) {
        this(profile, visitedStates, DEFAULT_MAX_VISITED_STATES);
    }

    public TransformationContext(ConstraintProfile profile, Set<String> visitedStates, int maxCapacity) {
        this.profile = Objects.requireNonNull(profile, "profile must not be null");
        this.visitedStates = Objects.requireNonNull(visitedStates, "visitedStates must not be null");
        this.maxCapacity = maxCapacity > 0 ? maxCapacity : DEFAULT_MAX_VISITED_STATES;
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
        if (visitedStates.contains(state)) {
            return false;
        }
        if (visitedStates.size() >= maxCapacity) {
            var it = visitedStates.iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
        return visitedStates.add(state);
    }
}
