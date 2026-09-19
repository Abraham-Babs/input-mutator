package com.inputmutator.engine.constraint;

import com.inputmutator.engine.model.ArtifactCategory;
import com.inputmutator.engine.model.GranularityMode;
import com.inputmutator.engine.model.InputType;
import com.inputmutator.engine.model.TestingIntent;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Declarative configuration of validation bounds, mutation parameters, and granularity.
 */
public record ConstraintProfile(
        InputType inputType,
        GranularityMode granularityMode,
        int maxPositionsMutated,
        boolean canonicalizePreEncoded,
        int encodingLayers,
        int minLength,
        int maxLength,
        boolean allowNonPrintable,
        boolean allowNullBytes,
        boolean preserveStructure,
        int maxDepth,
        int maxPermutations,
        Pattern allowedPattern,
        TestingIntent testingIntent,
        Set<ArtifactCategory> enabledCategories,
        CharacterSetConstraint characterSetConstraint
) {

    public static final int DEFAULT_MIN_LENGTH = 0;
    public static final int DEFAULT_MAX_LENGTH = 1024;
    public static final int DEFAULT_MAX_DEPTH = 2;
    public static final int DEFAULT_MAX_PERMUTATIONS = 150;
    public static final int DEFAULT_MAX_POSITIONS = 2;
    public static final int DEFAULT_ENCODING_LAYERS = 1;

    public boolean isUncapped() {
        return maxPermutations <= 0;
    }

    public boolean isCategoryEnabled(ArtifactCategory category) {
        return enabledCategories == null || enabledCategories.contains(category);
    }

    public static ConstraintProfile defaultProfile() {
        return new Builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
                .inputType(this.inputType)
                .granularityMode(this.granularityMode)
                .maxPositionsMutated(this.maxPositionsMutated)
                .canonicalizePreEncoded(this.canonicalizePreEncoded)
                .encodingLayers(this.encodingLayers)
                .minLength(this.minLength)
                .maxLength(this.maxLength)
                .allowNonPrintable(this.allowNonPrintable)
                .allowNullBytes(this.allowNullBytes)
                .preserveStructure(this.preserveStructure)
                .maxDepth(this.maxDepth)
                .maxPermutations(this.maxPermutations)
                .allowedPattern(this.allowedPattern)
                .testingIntent(this.testingIntent)
                .enabledCategories(this.enabledCategories)
                .characterSetConstraint(this.characterSetConstraint);
    }

    public static class Builder {
        private InputType inputType = InputType.GENERIC_STRING;
        private GranularityMode granularityMode = GranularityMode.TOKEN_ONLY;
        private int maxPositionsMutated = DEFAULT_MAX_POSITIONS;
        private boolean canonicalizePreEncoded = true;
        private int encodingLayers = DEFAULT_ENCODING_LAYERS;
        private int minLength = DEFAULT_MIN_LENGTH;
        private int maxLength = DEFAULT_MAX_LENGTH;
        private boolean allowNonPrintable = false;
        private boolean allowNullBytes = false;
        private boolean preserveStructure = true;
        private int maxDepth = DEFAULT_MAX_DEPTH;
        private int maxPermutations = DEFAULT_MAX_PERMUTATIONS;
        private Pattern allowedPattern = null;
        private TestingIntent testingIntent = TestingIntent.BLACKLIST_EVASION;
        private Set<ArtifactCategory> enabledCategories = EnumSet.allOf(ArtifactCategory.class);
        private CharacterSetConstraint characterSetConstraint = CharacterSetConstraint.ANY;

        public Builder inputType(InputType inputType) {
            this.inputType = inputType;
            return this;
        }

        public Builder granularityMode(GranularityMode mode) {
            this.granularityMode = (mode != null) ? mode : GranularityMode.TOKEN_ONLY;
            return this;
        }

        public Builder maxPositionsMutated(int maxPositions) {
            this.maxPositionsMutated = Math.max(1, Math.min(4, maxPositions));
            return this;
        }

        public Builder canonicalizePreEncoded(boolean canonicalize) {
            this.canonicalizePreEncoded = canonicalize;
            return this;
        }

        public Builder encodingLayers(int layers) {
            this.encodingLayers = Math.max(1, Math.min(3, layers));
            return this;
        }

        public Builder minLength(int minLength) {
            this.minLength = Math.max(0, minLength);
            return this;
        }

        public Builder maxLength(int maxLength) {
            this.maxLength = Math.max(1, maxLength);
            return this;
        }

        public Builder allowNonPrintable(boolean allowNonPrintable) {
            this.allowNonPrintable = allowNonPrintable;
            return this;
        }

        public Builder allowNullBytes(boolean allowNullBytes) {
            this.allowNullBytes = allowNullBytes;
            return this;
        }

        public Builder preserveStructure(boolean preserveStructure) {
            this.preserveStructure = preserveStructure;
            return this;
        }

        public Builder maxDepth(int maxDepth) {
            this.maxDepth = Math.max(1, maxDepth);
            return this;
        }

        public Builder maxPermutations(int maxPermutations) {
            this.maxPermutations = maxPermutations;
            return this;
        }

        public Builder allowedPattern(Pattern pattern) {
            this.allowedPattern = pattern;
            return this;
        }

        public Builder allowedPattern(String regex) {
            this.allowedPattern = (regex != null && !regex.isBlank()) ? Pattern.compile(regex) : null;
            return this;
        }

        public Builder testingIntent(TestingIntent intent) {
            if (intent != null) {
                this.testingIntent = intent;
                this.enabledCategories = EnumSet.copyOf(intent.defaultCategories());
            }
            return this;
        }

        public Builder enabledCategories(Set<ArtifactCategory> categories) {
            if (categories != null && !categories.isEmpty()) {
                this.enabledCategories = EnumSet.copyOf(categories);
            }
            return this;
        }

        public Builder characterSetConstraint(CharacterSetConstraint constraint) {
            this.characterSetConstraint = (constraint != null) ? constraint : CharacterSetConstraint.ANY;
            return this;
        }

        public ConstraintProfile build() {
            return new ConstraintProfile(
                    inputType,
                    granularityMode,
                    maxPositionsMutated,
                    canonicalizePreEncoded,
                    encodingLayers,
                    minLength,
                    maxLength,
                    allowNonPrintable,
                    allowNullBytes,
                    preserveStructure,
                    maxDepth,
                    maxPermutations,
                    allowedPattern,
                    testingIntent,
                    enabledCategories,
                    characterSetConstraint
            );
        }
    }
}
