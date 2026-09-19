package com.inputmutator.cli;

import com.inputmutator.engine.constraint.ConstraintProfile;
import com.inputmutator.engine.model.InputType;
import com.inputmutator.engine.pipeline.MutationEngine;

import java.util.List;

public class CliRunner {

    public static void run(String[] args) {
        if (args.length == 0 || hasFlag(args, "-h", "--help")) {
            printHelp();
            return;
        }

        String target = getOption(args, "-t", "--target");
        if (target == null) {
            target = "";
        }

        String typeStr = getOption(args, "--type");
        InputType inputType = parseInputType(typeStr);

        if (target.isBlank()) {
            System.err.println("[Info] Target input omitted. Generating boundary archetype seeds for "
                    + (inputType != null ? inputType : "GENERIC_STRING") + "...");
        }

        String granStr = getOption(args, "-g", "--granularity");
        com.inputmutator.engine.model.GranularityMode granularityMode = parseGranularityMode(granStr);

        int maxDepth = getIntOption(args, "--max-depth", ConstraintProfile.DEFAULT_MAX_DEPTH);
        int maxPermutations = getIntOption(args, "-n", "--max-permutations", ConstraintProfile.DEFAULT_MAX_PERMUTATIONS);
        int maxPositions = getIntOption(args, "--max-positions", ConstraintProfile.DEFAULT_MAX_POSITIONS);
        int encodingLayers = getIntOption(args, "--encoding-layers", ConstraintProfile.DEFAULT_ENCODING_LAYERS);
        boolean allowNonPrintable = hasFlag(args, "--allow-non-printable");
        boolean allowNullBytes = hasFlag(args, "--allow-null-bytes");
        boolean noCanonicalize = hasFlag(args, "--no-canonicalize");
        boolean noPreserveStructure = hasFlag(args, "--no-preserve-structure");

        ConstraintProfile profile = ConstraintProfile.builder()
                .inputType(inputType)
                .granularityMode(granularityMode)
                .maxPositionsMutated(maxPositions)
                .encodingLayers(encodingLayers)
                .canonicalizePreEncoded(!noCanonicalize)
                .preserveStructure(!noPreserveStructure)
                .maxDepth(maxDepth)
                .maxPermutations(maxPermutations)
                .allowNonPrintable(allowNonPrintable)
                .allowNullBytes(allowNullBytes)
                .build();

        MutationEngine engine = new MutationEngine();
        List<String> results = engine.generate(target, profile);

        if (results.isEmpty()) {
            System.err.println("[Notice] 0 permutations met the active constraints.");
        } else {
            for (String mutation : results) {
                System.out.println(mutation);
            }
        }
    }

    private static InputType parseInputType(String typeStr) {
        if (typeStr == null) return null;
        return switch (typeStr.toLowerCase()) {
            case "numeric", "number" -> InputType.NUMERIC;
            case "json" -> InputType.JSON;
            case "email" -> InputType.EMAIL;
            case "url", "url_path", "path" -> InputType.URL_PATH;
            case "phone", "phone_number" -> InputType.PHONE_NUMBER;
            case "generic", "string" -> InputType.GENERIC_STRING;
            default -> null;
        };
    }

    private static com.inputmutator.engine.model.GranularityMode parseGranularityMode(String granStr) {
        if (granStr == null) return com.inputmutator.engine.model.GranularityMode.TOKEN_ONLY;
        return switch (granStr.toLowerCase()) {
            case "single", "single_position", "char", "character" ->
                    com.inputmutator.engine.model.GranularityMode.SINGLE_POSITION;
            case "comb", "combinatorial", "multi" ->
                    com.inputmutator.engine.model.GranularityMode.COMBINATORIAL;
            default -> com.inputmutator.engine.model.GranularityMode.TOKEN_ONLY;
        };
    }

    private static boolean hasFlag(String[] args, String... flags) {
        for (String arg : args) {
            for (String flag : flags) {
                if (arg.equalsIgnoreCase(flag)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String getOption(String[] args, String... flags) {
        for (int i = 0; i < args.length; i++) {
            for (String flag : flags) {
                if (args[i].equalsIgnoreCase(flag) && i + 1 < args.length) {
                    return args[i + 1];
                }
            }
        }
        return null;
    }

    private static int getIntOption(String[] args, String flag, int defaultValue) {
        return getIntOption(args, flag, null, defaultValue);
    }

    private static int getIntOption(String[] args, String flag1, String flag2, int defaultValue) {
        for (int i = 0; i < args.length; i++) {
            if ((args[i].equalsIgnoreCase(flag1) || (flag2 != null && args[i].equalsIgnoreCase(flag2)))
                    && i + 1 < args.length) {
                try {
                    return Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException ignored) {}
            }
        }
        return defaultValue;
    }

    private static void printHelp() {
        System.out.println("Input Mutator - Constraint-Guided Permutation Engine");
        System.out.println("================================================================================");
        System.out.println("Designed for input validation auditing, parser boundary analysis, and fuzzing.");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java -jar input-mutator.jar                      # Launch Desktop GUI");
        System.out.println("  java -jar input-mutator.jar --help               # Show this help manual");
        System.out.println("  java -jar input-mutator.jar -t <input> [options] # Headless CLI execution");
        System.out.println();
        System.out.println("Core Options:");
        System.out.println("  -t, --target <string>       Input target string to mutate (required)");
        System.out.println("  --type <type>               Input semantics [generic, numeric, json, email, url, phone]");
        System.out.println("                              (Default: auto-detects from target syntax)");
        System.out.println("  -g, --granularity <mode>    Mutation granularity [token (default), single, combinatorial]");
        System.out.println("                              * token: mutates whole tokens (fast, compact)");
        System.out.println("                              * single: sliding window (1 char mutated at a time)");
        System.out.println("                              * combinatorial: multi-character pairs/subsets");
        System.out.println("  --max-positions <n>         Max simultaneous positions in combinatorial mode (1-4, def: 2)");
        System.out.println("  --encoding-layers <n>       Max nested encoding layers for escapes (1-3, def: 1)");
        System.out.println("  -n, --max-permutations <n>  Maximum unique mutations to output (default: 150)");
        System.out.println("  --max-depth <n>             Max recursive transformation depth (default: 2)");
        System.out.println();
        System.out.println("Boundary Flags:");
        System.out.println("  --allow-non-printable       Allow unescaped C0/C1 control codes and lone surrogates");
        System.out.println("  --allow-null-bytes          Allow raw unescaped NUL (\\0) bytes");
        System.out.println("  --no-canonicalize           Disable auto-canonicalization of pre-encoded inputs");
        System.out.println("  --no-preserve-structure     Allow mutations to break semantic delimiter boundaries");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # 1. Sliding-window keyword filter testing (mutate 1 char at a time):");
        System.out.println("  java -jar input-mutator.jar -t \"admin\" -g single -n 30");
        System.out.println();
        System.out.println("  # 2. Structured Email validation testing:");
        System.out.println("  java -jar input-mutator.jar -t \"user@corp.internal\" --type email");
        System.out.println();
        System.out.println("  # 3. Numeric radix and boundary testing (hex, scientific, overflows):");
        System.out.println("  java -jar input-mutator.jar -t \"42\" --type numeric -n 25");
        System.out.println();
        System.out.println("  # 4. Pipe permutations directly into other tools or wordlists:");
        System.out.println("  java -jar input-mutator.jar -t \"../secret\" -n 50 > payloads.txt");
        System.out.println("================================================================================");
    }
}
