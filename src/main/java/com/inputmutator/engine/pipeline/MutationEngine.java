package com.inputmutator.engine.pipeline;

import com.inputmutator.engine.constraint.ConstraintProfile;
import com.inputmutator.engine.constraint.ConstraintValidator;
import com.inputmutator.engine.encoding.Canonicalizer;
import com.inputmutator.engine.encoding.EncodingDetector;
import com.inputmutator.engine.model.GranularityMode;
import com.inputmutator.engine.model.InputType;
import com.inputmutator.engine.pipeline.mutators.BoundaryMutator;
import com.inputmutator.engine.pipeline.mutators.EscapingMutator;
import com.inputmutator.engine.pipeline.mutators.NonPrintableMutator;
import com.inputmutator.engine.pipeline.mutators.NumericRadixMutator;
import com.inputmutator.engine.pipeline.mutators.UnicodeMutator;
import com.inputmutator.engine.tokenizer.InputTokenizer;
import com.inputmutator.engine.tokenizer.Token;
import com.inputmutator.engine.tokenizer.TokenType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Orchestrator coordinating tokenization, mutator dispatch, stateful pruning, and constraint validation.
 */
public class MutationEngine {

    private final InputTokenizer tokenizer;
    private final ConstraintValidator validator;
    private final List<Mutator> mutators;
    private final SeedGenerator seedGenerator;
    private final EncodingDetector encodingDetector;
    private final Canonicalizer canonicalizer;

    public static final int MAX_INPUT_CEILING = 16384;

    public MutationEngine() {
        this(
                new InputTokenizer(),
                new ConstraintValidator(),
                List.of(
                        new UnicodeMutator(),
                        new NumericRadixMutator(),
                        new EscapingMutator(),
                        new NonPrintableMutator(),
                        new BoundaryMutator()
                )
        );
    }

    public MutationEngine(InputTokenizer tokenizer, ConstraintValidator validator, List<Mutator> mutators) {
        this.tokenizer = tokenizer;
        this.validator = validator;
        this.mutators = mutators;
        this.seedGenerator = new SeedGenerator();
        this.encodingDetector = new EncodingDetector();
        this.canonicalizer = new Canonicalizer();
    }

    /**
     * Generates constraint-guided permutations for the given input, or produces archetype seeds if empty.
     */
    public List<String> generate(String input, ConstraintProfile profile) {
        // Zero-input scenario: generate archetype seeds for chosen profile
        if (input == null || input.isBlank()) {
            List<String> seeds = seedGenerator.generateSeeds(profile);
            Set<String> aggregated = new LinkedHashSet<>();
            for (String seed : seeds) {
                if (validator.isValid(seed, profile)) {
                    aggregated.add(seed);
                }
                if (aggregated.size() >= profile.maxPermutations()) {
                    break;
                }
            }
            return new ArrayList<>(aggregated);
        }

        if (input.length() > MAX_INPUT_CEILING) {
            throw new IllegalArgumentException(
                    "Input length (" + input.length() + ") exceeds maximum safety threshold of " + MAX_INPUT_CEILING + " characters."
            );
        }

        // Auto-canonicalize pre-encoded inputs if enabled
        String targetInput = input;
        if (profile.canonicalizePreEncoded() && encodingDetector.isEncoded(input)) {
            targetInput = canonicalizer.canonicalize(input);
        }

        InputType effectiveType = (profile.inputType() == null || profile.inputType() == InputType.GENERIC_STRING)
                ? InputType.detect(targetInput)
                : profile.inputType();

        ConstraintProfile activeProfile = ConstraintProfile.builder()
                .inputType(effectiveType)
                .granularityMode(profile.granularityMode())
                .maxPositionsMutated(profile.maxPositionsMutated())
                .canonicalizePreEncoded(profile.canonicalizePreEncoded())
                .encodingLayers(profile.encodingLayers())
                .minLength(profile.minLength())
                .maxLength(profile.maxLength())
                .allowNonPrintable(profile.allowNonPrintable())
                .allowNullBytes(profile.allowNullBytes())
                .preserveStructure(profile.preserveStructure())
                .maxDepth(profile.maxDepth())
                .maxPermutations(profile.maxPermutations())
                .allowedPattern(profile.allowedPattern())
                .build();

        TransformationContext context = new TransformationContext(activeProfile);
        Set<String> resultSet = new LinkedHashSet<>();
        context.markVisited(targetInput);

        GranularityMode mode = activeProfile.granularityMode();
        if (mode == GranularityMode.SINGLE_POSITION) {
            generateSinglePosition(targetInput, activeProfile, context, resultSet);
        } else if (mode == GranularityMode.COMBINATORIAL) {
            generateCombinatorial(targetInput, activeProfile, context, resultSet);
        } else {
            generateTokenBfs(targetInput, activeProfile, effectiveType, context, resultSet);
        }

        return new ArrayList<>(resultSet);
    }

    private void generateTokenBfs(String input, ConstraintProfile activeProfile, InputType effectiveType,
                                  TransformationContext context, Set<String> resultSet) {
        record QueueItem(String text, int depth) {}
        Queue<QueueItem> queue = new ArrayDeque<>();
        queue.add(new QueueItem(input, 0));

        while (!queue.isEmpty() && resultSet.size() < activeProfile.maxPermutations()) {
            QueueItem current = queue.poll();
            List<Token> tokens = tokenizer.tokenize(current.text(), effectiveType);

            for (Token token : tokens) {
                for (Mutator mutator : mutators) {
                    if (!mutator.appliesTo(token, context)) {
                        continue;
                    }

                    List<String> variations = mutator.mutate(token, context);
                    for (String var : variations) {
                        String candidate = current.text().substring(0, token.startIndex())
                                + var
                                + current.text().substring(token.endIndex());

                        if (!context.markVisited(candidate)) {
                            continue;
                        }

                        if (validator.isValid(candidate, activeProfile)) {
                            resultSet.add(candidate);
                            if (resultSet.size() >= activeProfile.maxPermutations()) {
                                break;
                            }
                        }

                        if (current.depth() + 1 < activeProfile.maxDepth()) {
                            queue.add(new QueueItem(candidate, current.depth() + 1));
                        }
                    }

                    if (resultSet.size() >= activeProfile.maxPermutations()) break;
                }
                if (resultSet.size() >= activeProfile.maxPermutations()) break;
            }
        }
    }

    private void generateSinglePosition(String input, ConstraintProfile profile,
                                        TransformationContext context, Set<String> resultSet) {
        List<Token> tokens = tokenizer.tokenize(input, profile.inputType());

        for (Token token : tokens) {
            if (!isPositionCandidate(token)) {
                continue;
            }

            int[] codePoints = token.value().codePoints().toArray();
            int currentOffset = token.startIndex();

            for (int i = 0; i < codePoints.length; i++) {
                int cp = codePoints[i];
                String charStr = new String(Character.toChars(cp));
                int charLen = charStr.length();
                Token singleCharToken = new Token(token.type(), charStr, currentOffset, currentOffset + charLen);

                for (Mutator mutator : mutators) {
                    if (!mutator.appliesTo(singleCharToken, context)) {
                        continue;
                    }

                    List<String> variations = mutator.mutate(singleCharToken, context);
                    for (String var : variations) {
                        String candidate = input.substring(0, currentOffset)
                                + var
                                + input.substring(currentOffset + charLen);

                        if (context.markVisited(candidate) && validator.isValid(candidate, profile)) {
                            resultSet.add(candidate);
                            if (resultSet.size() >= profile.maxPermutations()) {
                                return;
                            }
                        }

                        // Layered multi-encoding on this single character if requested
                        if (profile.encodingLayers() > 1 && var.startsWith("%")) {
                            String doubleEncoded = input.substring(0, currentOffset)
                                    + "%25" + var.substring(1)
                                    + input.substring(currentOffset + charLen);
                            if (context.markVisited(doubleEncoded) && validator.isValid(doubleEncoded, profile)) {
                                resultSet.add(doubleEncoded);
                            }
                        }
                    }
                }
                currentOffset += charLen;
            }
        }
    }

    private void generateCombinatorial(String input, ConstraintProfile profile,
                                       TransformationContext context, Set<String> resultSet) {
        generateSinglePosition(input, profile, context, resultSet);
        if (resultSet.size() >= profile.maxPermutations()) {
            return;
        }

        List<Token> tokens = tokenizer.tokenize(input, profile.inputType());
        for (Token token : tokens) {
            if (!isPositionCandidate(token)) continue;

            int[] codePoints = token.value().codePoints().toArray();
            if (codePoints.length < 2) continue;

            int limit = Math.min(codePoints.length, 12);
            for (int i = 0; i < limit; i++) {
                for (int j = i + 1; j < limit; j++) {
                    String charI = new String(Character.toChars(codePoints[i]));
                    String charJ = new String(Character.toChars(codePoints[j]));

                    Token tokenI = new Token(token.type(), charI, 0, charI.length());
                    Token tokenJ = new Token(token.type(), charJ, 0, charJ.length());

                    // Heterogeneous mutations: position I and position J pull different transforms
                    List<String> varsI = getQuickVariations(tokenI, context, 0);
                    List<String> varsJ = getQuickVariations(tokenJ, context, 1);

                    for (String vi : varsI) {
                        for (String vj : varsJ) {
                            StringBuilder sb = new StringBuilder();
                            int cpIdx = 0;
                            for (int cp : codePoints) {
                                if (cpIdx == i) {
                                    sb.append(vi);
                                } else if (cpIdx == j) {
                                    sb.append(vj);
                                } else {
                                    sb.append(Character.toChars(cp));
                                }
                                cpIdx++;
                            }

                            String mutatedTokenVal = sb.toString();
                            String candidate = input.substring(0, token.startIndex())
                                    + mutatedTokenVal
                                    + input.substring(token.endIndex());

                            if (context.markVisited(candidate) && validator.isValid(candidate, profile)) {
                                resultSet.add(candidate);
                                if (resultSet.size() >= profile.maxPermutations()) {
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private List<String> getQuickVariations(Token singleToken, TransformationContext context, int offset) {
        List<String> quick = new ArrayList<>();
        int mutatorCount = mutators.size();
        for (int i = 0; i < mutatorCount; i++) {
            Mutator m = mutators.get((i + offset) % mutatorCount);
            if (m.appliesTo(singleToken, context)) {
                List<String> vars = m.mutate(singleToken, context);
                for (String v : vars) {
                    quick.add(v);
                    if (quick.size() >= 3) break;
                }
            }
            if (quick.size() >= 5) break;
        }
        return quick;
    }

    private boolean isPositionCandidate(Token token) {
        TokenType t = token.type();
        return t == TokenType.LITERAL || t == TokenType.EMAIL_LOCAL ||
               t == TokenType.JSON_VALUE || t == TokenType.SPECIAL_CHAR;
    }
}
