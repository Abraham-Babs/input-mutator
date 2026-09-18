package com.inputmutator.engine.pipeline;

import com.inputmutator.engine.constraint.ConstraintProfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Stateful tracking session across transformation steps to eliminate cycles and redundant encodings.
 */
public class TransformationContext {

    private final ConstraintProfile profile;
    private final int depth;
    private final Set<String> visitedStates;
    private final List<String> lineage;

    public TransformationContext(ConstraintProfile profile) {
        this(profile, 0, new HashSet<>(), new ArrayList<>());
    }

    private TransformationContext(ConstraintProfile profile, int depth, Set<String> visitedStates, List<String> lineage) {
        this.profile = profile;
        this.depth = depth;
        this.visitedStates = visitedStates;
        this.lineage = lineage;
    }

    public ConstraintProfile profile() {
        return profile;
    }

    public int depth() {
        return depth;
    }

    public boolean canDescend() {
        return depth < profile.maxDepth();
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

    public boolean isVisited(String state) {
        return visitedStates.contains(state);
    }

    public int visitedCount() {
        return visitedStates.size();
    }

    public TransformationContext nextDepth(String transformName) {
        List<String> nextLineage = new ArrayList<>(lineage);
        nextLineage.add(transformName);
        return new TransformationContext(profile, depth + 1, visitedStates, nextLineage);
    }

    public List<String> lineage() {
        return Collections.unmodifiableList(lineage);
    }
}
