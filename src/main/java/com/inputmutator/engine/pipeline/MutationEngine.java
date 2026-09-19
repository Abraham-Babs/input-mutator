package com.inputmutator.engine.pipeline;

import com.inputmutator.engine.constraint.ConstraintProfile;
import com.inputmutator.engine.constraint.ConstraintValidator;
import com.inputmutator.engine.encoding.Canonicalizer;
import com.inputmutator.engine.encoding.EncodingDetector;
import com.inputmutator.engine.model.GranularityMode;
import com.inputmutator.engine.model.InputType;
import com.inputmutator.engine.pipeline.mutators.BoundaryMutator;
import com.inputmutator.engine.pipeline.mutators.EmailDifferentialMutator;
import com.inputmutator.engine.pipeline.mutators.EscapingMutator;
import com.inputmutator.engine.pipeline.mutators.JsonDifferentialMutator;
import com.inputmutator.engine.pipeline.mutators.NonPrintableMutator;
import com.inputmutator.engine.pipeline.mutators.NumericRadixMutator;
import com.inputmutator.engine.pipeline.mutators.UnicodeMutator;
import com.inputmutator.engine.pipeline.mutators.UrlDifferentialMutator;
import com.inputmutator.engine.tokenizer.InputTokenizer;
import com.inputmutator.engine.tokenizer.Token;
import com.inputmutator.engine.tokenizer.TokenType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

/**
 * Orchestrator coordinating tokenization, mutator dispatch, stateful pruning, and constraint validation.
 */
public class MutationEngine {

    public static final int MAX_INPUT_CEILING = 16384;
    public static final int DEFAULT_MAX_QUEUE_SIZE = 4096;
    private static final int MAX_COMBINATORIAL_CODEPOINTS = 12;

    private final InputTokenizer tokenizer;
    private final ConstraintValidator validator;
    private final List<Mutator> mutators;
    private final SeedGenerator seedGenerator;
    private final EncodingDetector encodingDetector;
    private final Canonicalizer canonicalizer;
    private final StrategyDispatcher strategyDispatcher;
    private final LayeredCompositionEngine layeredCompositionEngine;

    public MutationEngine() {
        this(
                new InputTokenizer(),
                new ConstraintValidator(),
                List.of(
                        new UnicodeMutator(),
                        new NumericRadixMutator(),
                        new EscapingMutator(),
                        new NonPrintableMutator(),
                        new BoundaryMutator(),
                        new UrlDifferentialMutator(),
                        new EmailDifferentialMutator(),
                        new JsonDifferentialMutator()
                )
        );
    }

    public MutationEngine(InputTokenizer tokenizer, ConstraintValidator validator, List<Mutator> mutators) {
        this(tokenizer, validator, mutators, new SeedGenerator(), new EncodingDetector(), new Canonicalizer());
    }

    public MutationEngine(
            InputTokenizer tokenizer,
            ConstraintValidator validator,
            List<Mutator> mutators,
            SeedGenerator seedGenerator,
            EncodingDetector encodingDetector,
            Canonicalizer canonicalizer
    ) {
        this(tokenizer, validator, mutators, seedGenerator, encodingDetector, canonicalizer, new StrategyDispatcher(), new LayeredCompositionEngine());
    }

    public MutationEngine(
            InputTokenizer tokenizer,
            ConstraintValidator validator,
            List<Mutator> mutators,
            SeedGenerator seedGenerator,
            EncodingDetector encodingDetector,
            Canonicalizer canonicalizer,
            StrategyDispatcher strategyDispatcher,
            LayeredCompositionEngine layeredCompositionEngine
    ) {
        this.tokenizer = Objects.requireNonNull(tokenizer, "tokenizer must not be null");
        this.validator = Objects.requireNonNull(validator, "validator must not be null");
        this.mutators = List.copyOf(Objects.requireNonNull(mutators, "mutators must not be null"));
        this.seedGenerator = Objects.requireNonNull(seedGenerator, "seedGenerator must not be null");
        this.encodingDetector = Objects.requireNonNull(encodingDetector, "encodingDetector must not be null");
        this.canonicalizer = Objects.requireNonNull(canonicalizer, "canonicalizer must not be null");
        this.strategyDispatcher = Objects.requireNonNull(strategyDispatcher, "strategyDispatcher must not be null");
        this.layeredCompositionEngine = Objects.requireNonNull(layeredCompositionEngine, "layeredCompositionEngine must not be null");
    }

    /**
     * Generates constraint-guided permutations for the given input, or produces archetype seeds if empty.
     */
    public List<String> generate(String input, ConstraintProfile profile) {
        Objects.requireNonNull(profile, "profile must not be null");

        // Zero-input scenario: generate archetype seeds and mutate each through the active pipeline
        if (input == null || input.isBlank()) {
            List<String> seeds = seedGenerator.generateSeeds(profile);
            Set<String> aggregated = new LinkedHashSet<>();

            // Step 1: Collect valid base archetype seeds
            for (String seed : seeds) {
                if (validator.isValid(seed, profile)) {
                    aggregated.add(seed);
                }
                if (isLimitReached(aggregated.size(), profile)) {
                    return new ArrayList<>(aggregated);
                }
            }

            // Step 2: Actively mutate each seed through the engine to populate full permutation quota
            for (String seed : seeds) {
                if (isLimitReached(aggregated.size(), profile)) {
                    break;
                }
                List<String> seedPermutations = generateForTarget(seed, profile);
                for (String perm : seedPermutations) {
                    if (validator.isValid(perm, profile)) {
                        aggregated.add(perm);
                    }
                    if (isLimitReached(aggregated.size(), profile)) {
                        break;
                    }
                }
            }
            return new ArrayList<>(aggregated);
        }

        return generateForTarget(input, profile);
    }

    private List<String> generateForTarget(String input, ConstraintProfile profile) {
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

        ConstraintProfile activeProfile = profile.toBuilder()
                .inputType(effectiveType)
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

    private boolean isLimitReached(int currentSize, ConstraintProfile profile) {
        return !profile.isUncapped() && currentSize >= profile.maxPermutations();
    }

    private void generateTokenBfs(String input, ConstraintProfile activeProfile, InputType effectiveType,
                                  TransformationContext context, Set<String> resultSet) {
        record QueueItem(String text, int depth) {}
        Queue<QueueItem> queue = new ArrayDeque<>();
        queue.add(new QueueItem(input, 0));

        while (!queue.isEmpty() && !isLimitReached(resultSet.size(), activeProfile)) {
            QueueItem current = queue.poll();
            List<Token> tokens = tokenizer.tokenize(current.text(), effectiveType);

            for (Token token : tokens) {
                List<List<String>> mutatorVariations = new ArrayList<>();
                List<Mutator> activeMutators = strategyDispatcher.filterApplicableMutators(mutators, token, context);
                for (Mutator mutator : activeMutators) {
                    List<String> vars = mutator.mutate(token, context);
                    if (!vars.isEmpty()) {
                        mutatorVariations.add(vars);
                    }
                }
                if (context.profile().encodingLayers() >= 2) {
                    List<String> layered = layeredCompositionEngine.composeLayers(token, context, activeMutators);
                    if (!layered.isEmpty()) {
                        mutatorVariations.add(layered);
                    }
                }

                int maxVars = mutatorVariations.stream().mapToInt(List::size).max().orElse(0);
                for (int round = 0; round < maxVars; round++) {
                    for (List<String> vars : mutatorVariations) {
                        if (round < vars.size()) {
                            String var = vars.get(round);
                            String candidate = current.text().substring(0, token.startIndex())
                                    + var
                                    + current.text().substring(token.endIndex());

                            if (!context.markVisited(candidate)) {
                                continue;
                            }

                            if (validator.isValid(candidate, activeProfile)) {
                                resultSet.add(candidate);
                                if (isLimitReached(resultSet.size(), activeProfile)) {
                                    return;
                                }
                            }

                            int nextDepth = current.depth() + 1;
                            if (nextDepth < activeProfile.maxDepth() && queue.size() < DEFAULT_MAX_QUEUE_SIZE) {
                                queue.add(new QueueItem(candidate, nextDepth));
                            }
                        }
                    }
                    if (isLimitReached(resultSet.size(), activeProfile)) {
                        return;
                    }
                }
            }
        }
    }

    private void generateSinglePosition(String input, ConstraintProfile profile,
                                        TransformationContext context, Set<String> resultSet) {
        List<Token> tokens = tokenizer.tokenize(input, profile.inputType());

        for (Token token : tokens) {
            if (!isPositionCandidate(token, profile)) {
                continue;
            }

            int[] codePoints = token.value().codePoints().toArray();
            int currentOffset = token.startIndex();
            List<List<String>> positionCandidates = new ArrayList<>(codePoints.length);

            for (int cp : codePoints) {
                String charStr = new String(Character.toChars(cp));
                int charLen = charStr.length();
                Token singleCharToken = new Token(token.type(), charStr, currentOffset, currentOffset + charLen);
                List<String> posList = new ArrayList<>();

                List<Mutator> activeMutators = strategyDispatcher.filterApplicableMutators(mutators, singleCharToken, context, true);
                for (Mutator mutator : activeMutators) {
                    List<String> variations = mutator.mutate(singleCharToken, context);
                    for (String var : variations) {
                        String candidate = input.substring(0, currentOffset)
                                + var
                                + input.substring(currentOffset + charLen);
                        posList.add(candidate);
                    }
                }
                if (context.profile().encodingLayers() >= 2) {
                    List<String> layered = layeredCompositionEngine.composeLayers(singleCharToken, context, activeMutators);
                    for (String var : layered) {
                        String candidate = input.substring(0, currentOffset)
                                + var
                                + input.substring(currentOffset + charLen);
                        posList.add(candidate);
                    }
                }
                positionCandidates.add(posList);
                currentOffset += charLen;
            }

            // Fair round-robin interleaving across all character positions
            int maxPerPos = positionCandidates.stream().mapToInt(List::size).max().orElse(0);
            for (int round = 0; round < maxPerPos; round++) {
                for (List<String> posList : positionCandidates) {
                    if (round < posList.size()) {
                        String candidate = posList.get(round);
                        if (context.markVisited(candidate) && validator.isValid(candidate, profile)) {
                            resultSet.add(candidate);
                            if (isLimitReached(resultSet.size(), profile)) {
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private void generateCombinatorial(String input, ConstraintProfile profile,
                                       TransformationContext context, Set<String> resultSet) {
        Set<String> singleCandidates = new LinkedHashSet<>();
        generateSinglePosition(input, profile, context, singleCandidates);

        Set<String> comboCandidates = new LinkedHashSet<>();
        List<Token> tokens = tokenizer.tokenize(input, profile.inputType());
        for (Token token : tokens) {
            if (!isPositionCandidate(token, profile)) continue;

            // 1. All-character simultaneous mutations (full token transforms)
            if (token.value().length() > 1) {
                List<Mutator> activeMutators = strategyDispatcher.filterApplicableMutators(mutators, token, context);
                for (Mutator mutator : activeMutators) {
                    for (String wholeVar : mutator.mutate(token, context)) {
                        String candidate = input.substring(0, token.startIndex())
                                + wholeVar
                                + input.substring(token.endIndex());
                        if (context.markVisited(candidate) && validator.isValid(candidate, profile)) {
                            comboCandidates.add(candidate);
                        }
                    }
                }
                if (context.profile().encodingLayers() >= 2) {
                    for (String layered : layeredCompositionEngine.composeLayers(token, context, activeMutators)) {
                        String candidate = input.substring(0, token.startIndex())
                                + layered
                                + input.substring(token.endIndex());
                        if (context.markVisited(candidate) && validator.isValid(candidate, profile)) {
                            comboCandidates.add(candidate);
                        }
                    }
                }
            }

            // 2. Multi-character combinations across index pairs/triplets
            int[] codePoints = token.value().codePoints().toArray();
            if (codePoints.length < 2 || profile.maxPositionsMutated() < 2) continue;

            int limit = Math.min(codePoints.length, MAX_COMBINATORIAL_CODEPOINTS);
            int maxPositions = Math.min(profile.maxPositionsMutated(), limit);

            List<List<String>> allComboCandidates = new ArrayList<>();

            for (int k = 2; k <= maxPositions; k++) {
                List<int[]> combinations = getIndexCombinations(limit, k);
                for (int[] combo : combinations) {
                    List<List<String>> comboVariations = new ArrayList<>();
                    for (int pos = 0; pos < combo.length; pos++) {
                        String ch = new String(Character.toChars(codePoints[combo[pos]]));
                        Token charToken = new Token(token.type(), ch, 0, ch.length());
                        List<String> vars = getQuickVariations(charToken, context, pos);
                        if (vars.isEmpty()) {
                            comboVariations.clear();
                            break;
                        }
                        comboVariations.add(vars);
                    }

                    if (comboVariations.isEmpty()) {
                        continue;
                    }

                    List<String> currentCombos = new ArrayList<>();
                    for (String[] chosenVars : productCombinations(comboVariations)) {
                        StringBuilder sb = new StringBuilder();
                        int cpIdx = 0;
                        for (int cp : codePoints) {
                            int matchedIdx = -1;
                            for (int m = 0; m < combo.length; m++) {
                                if (combo[m] == cpIdx) {
                                    matchedIdx = m;
                                    break;
                                }
                            }
                            if (matchedIdx >= 0) {
                                sb.append(chosenVars[matchedIdx]);
                            } else {
                                sb.append(Character.toChars(cp));
                            }
                            cpIdx++;
                        }

                        currentCombos.add(input.substring(0, token.startIndex())
                                + sb.toString()
                                + input.substring(token.endIndex()));
                    }

                    if (!currentCombos.isEmpty()) {
                        allComboCandidates.add(currentCombos);
                    }
                }
            }

            int maxComboVars = allComboCandidates.stream().mapToInt(List::size).max().orElse(0);
            for (int round = 0; round < maxComboVars; round++) {
                for (List<String> comboList : allComboCandidates) {
                    if (round < comboList.size()) {
                        String candidate = comboList.get(round);
                        if (context.markVisited(candidate) && validator.isValid(candidate, profile)) {
                            comboCandidates.add(candidate);
                        }
                    }
                }
            }
        }

        // Fair round-robin interleaving: guarantee multi-position mutations are balanced with single-char
        var comboIt = comboCandidates.iterator();
        var singleIt = singleCandidates.iterator();
        while ((comboIt.hasNext() || singleIt.hasNext()) && !isLimitReached(resultSet.size(), profile)) {
            if (comboIt.hasNext()) {
                resultSet.add(comboIt.next());
                if (isLimitReached(resultSet.size(), profile)) break;
            }
            if (singleIt.hasNext()) {
                resultSet.add(singleIt.next());
            }
        }
    }

    private List<int[]> getIndexCombinations(int n, int k) {
        List<int[]> result = new ArrayList<>();
        buildCombinations(0, 0, n, k, new int[k], result);
        return result;
    }

    private void buildCombinations(int start, int depth, int n, int k, int[] current, List<int[]> result) {
        if (depth == k) {
            result.add(current.clone());
            return;
        }
        for (int i = start; i <= n - (k - depth); i++) {
            current[depth] = i;
            buildCombinations(i + 1, depth + 1, n, k, current, result);
            if (result.size() >= 64) {
                break;
            }
        }
    }

    private List<String[]> productCombinations(List<List<String>> lists) {
        List<String[]> result = new ArrayList<>();
        generateProduct(lists, 0, new String[lists.size()], result);
        return result;
    }

    private void generateProduct(List<List<String>> lists, int depth, String[] current, List<String[]> result) {
        if (depth == lists.size()) {
            result.add(current.clone());
            return;
        }
        for (String item : lists.get(depth)) {
            current[depth] = item;
            generateProduct(lists, depth + 1, current, result);
            if (result.size() >= 16) {
                break;
            }
        }
    }

    private List<String> getQuickVariations(Token singleToken, TransformationContext context, int offset) {
        List<String> quick = new ArrayList<>();
        List<Mutator> activeMutators = strategyDispatcher.filterApplicableMutators(mutators, singleToken, context, true);
        int mutatorCount = activeMutators.size();
        if (mutatorCount == 0) return quick;
        for (int i = 0; i < mutatorCount; i++) {
            Mutator m = activeMutators.get((i + offset) % mutatorCount);
            List<String> vars = m.mutate(singleToken, context);
            for (String v : vars) {
                quick.add(v);
                if (quick.size() >= 3) break;
            }
            if (quick.size() >= 5) break;
        }
        return quick;
    }

    private boolean isPositionCandidate(Token token, ConstraintProfile profile) {
        TokenType t = token.type();
        if (t == TokenType.LITERAL || t == TokenType.NUMERIC || t == TokenType.EMAIL_LOCAL ||
            t == TokenType.JSON_VALUE || t == TokenType.JSON_KEY || t == TokenType.SPECIAL_CHAR) {
            return true;
        }
        return !profile.preserveStructure() && t == TokenType.DELIMITER;
    }
}
