package com.inputmutator.cli;

import com.inputmutator.engine.constraint.CharacterSetConstraint;
import com.inputmutator.engine.constraint.ConstraintProfile;
import com.inputmutator.engine.model.ArtifactCategory;
import com.inputmutator.engine.model.GranularityMode;
import com.inputmutator.engine.model.InputType;
import com.inputmutator.engine.model.TestingIntent;
import com.inputmutator.engine.pipeline.MutationEngine;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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
        GranularityMode granularityMode = parseGranularityMode(granStr);

        String intentStr = getOption(args, "--intent", "-i");
        TestingIntent intent = parseTestingIntent(intentStr);

        String charsetStr = getOption(args, "--charset", "-c");
        CharacterSetConstraint charsetConstraint = parseCharacterSetConstraint(charsetStr);

        String catStr = getOption(args, "--categories");
        Set<ArtifactCategory> categories = parseCategories(catStr, intent);

        int maxDepth = getIntOption(args, "--max-depth", ConstraintProfile.DEFAULT_MAX_DEPTH);
        int maxPermutations = getIntOption(args, "-n", "--max-permutations", ConstraintProfile.DEFAULT_MAX_PERMUTATIONS);
        int maxPositions = getIntOption(args, "--max-positions", ConstraintProfile.DEFAULT_MAX_POSITIONS);
        int encodingLayers = getIntOption(args, "--encoding-layers", ConstraintProfile.DEFAULT_ENCODING_LAYERS);
        boolean allowNonPrintable = hasFlag(args, "--allow-non-printable");
        boolean allowNullBytes = hasFlag(args, "--allow-null-bytes");
        boolean noCanonicalize = hasFlag(args, "--no-canonicalize");
        boolean noPreserveStructure = hasFlag(args, "--no-preserve-structure");

        var builder = ConstraintProfile.builder()
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
                .testingIntent(intent)
                .characterSetConstraint(charsetConstraint);

        if (categories != null) {
            builder.enabledCategories(categories);
        }

        ConstraintProfile profile = builder.build();

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

    private static TestingIntent parseTestingIntent(String intentStr) {
        if (intentStr == null) return TestingIntent.BLACKLIST_EVASION;
        return switch (intentStr.toLowerCase()) {
            case "whitelist", "whitelist_auditing", "audit" -> TestingIntent.WHITELIST_AUDITING;
            case "differential", "parser_differential", "diff" -> TestingIntent.PARSER_DIFFERENTIAL;
            case "blacklist", "blacklist_evasion", "evasion" -> TestingIntent.BLACKLIST_EVASION;
            default -> TestingIntent.BLACKLIST_EVASION;
        };
    }

    private static CharacterSetConstraint parseCharacterSetConstraint(String charsetStr) {
        if (charsetStr == null) return CharacterSetConstraint.ANY;
        return switch (charsetStr.toLowerCase()) {
            case "ascii", "ascii_only" -> CharacterSetConstraint.ASCII_ONLY;
            case "alphanumeric", "alnum" -> CharacterSetConstraint.ALPHANUMERIC;
            case "printable", "printable_ascii" -> CharacterSetConstraint.PRINTABLE_ASCII;
            default -> CharacterSetConstraint.ANY;
        };
    }

    private static Set<ArtifactCategory> parseCategories(String catStr, TestingIntent intent) {
        if (catStr == null || catStr.isBlank()) {
            return intent != null ? intent.defaultCategories() : null;
        }
        Set<ArtifactCategory> set = EnumSet.noneOf(ArtifactCategory.class);
        for (String part : catStr.split(",")) {
            String trimmed = part.trim().toLowerCase();
            switch (trimmed) {
                case "unicode", "homoglyph", "unicode_homoglyphs" -> set.add(ArtifactCategory.UNICODE_HOMOGLYPHS);
                case "url", "url_encoding" -> set.add(ArtifactCategory.URL_ENCODING);
                case "html", "html_entities" -> set.add(ArtifactCategory.HTML_ENTITIES);
                case "overlong", "overlong_utf8" -> set.add(ArtifactCategory.OVERLONG_UTF8);
                case "radix", "numeric", "numeric_radix" -> set.add(ArtifactCategory.NUMERIC_RADIX);
                case "grammar", "differential", "grammar_differential" -> set.add(ArtifactCategory.GRAMMAR_DIFFERENTIAL);
                case "control", "non_printable", "control_non_printable" -> set.add(ArtifactCategory.CONTROL_NON_PRINTABLE);
                default -> {}
            }
        }
        return set.isEmpty() ? (intent != null ? intent.defaultCategories() : null) : set;
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

    private static GranularityMode parseGranularityMode(String granStr) {
        if (granStr == null) return GranularityMode.TOKEN_ONLY;
        return switch (granStr.toLowerCase()) {
            case "single", "single_position", "char", "character" ->
                    GranularityMode.SINGLE_POSITION;
            case "comb", "combinatorial", "multi" ->
                    GranularityMode.COMBINATORIAL;
            default -> GranularityMode.TOKEN_ONLY;
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
        System.out.println("  -t, --target <string>       Input target string to mutate. If omitted, the engine");
        System.out.println("                              synthesizes archetype boundary seeds for the target type.");
        System.out.println("  --type <type>               Input semantic structure [generic, numeric, json, email, url, phone]");
        System.out.println("                              Preserves valid syntax boundaries while mutating internal tokens.");
        System.out.println("                              (Default: auto-detects from target syntax)");
        System.out.println();
        System.out.println("Operational Strategy & Context:");
        System.out.println("  -i, --intent <intent>       Testing intent presets [whitelist, blacklist (default), differential]:");
        System.out.println("                              * whitelist:    Strict spec compliance; tests normalization expansion.");
        System.out.println("                              * blacklist:    Obfuscates forbidden keywords to evade signature filters.");
        System.out.println("                              * differential: Proxy/parser desync via matrix params, comments, slashes.");
        System.out.println("  -c, --charset <charset>     Declarative character constraint [any (default), ascii, alphanumeric, printable]:");
        System.out.println("                              * any:          Full Unicode spectrum without restriction.");
        System.out.println("                              * ascii:        Restricts mutations strictly to ASCII (0x00 - 0x7F).");
        System.out.println("                              * alphanumeric: Enforces strict alphanumeric boundary (A-Z, a-z, 0-9).");
        System.out.println("                              * printable:    Limits output to printable ASCII (0x20 - 0x7E).");
        System.out.println("  --categories <cat1,cat2>    Comma-separated list of active artifact vector categories:");
        System.out.println("                              * url:      URL percent-encoding (%xx, double %25xx, triple %2525xx)");
        System.out.println("                              * overlong: Multi-byte overlong UTF-8 encodings (%c0%af, %e0%80%af)");
        System.out.println("                              * unicode:  Unicode confusables, Cyrillic/Greek, NFKC expansions");
        System.out.println("                              * html:     HTML decimal (&#0047;), hex (&#x2F;), and named entities");
        System.out.println("                              * radix:    Alternate radices (0x, 0o, 0b), scientific, IEEE 754 NaN");
        System.out.println("                              * grammar:  Path matrix params (;/;param=1), dot-segments, comments");
        System.out.println("                              * control:  Zero-width spaces, C0/C1 control codes, and raw null bytes");
        System.out.println();
        System.out.println("Granularity & Permutation Scope:");
        System.out.println("  -g, --granularity <mode>    Mutation scope [token (default), single, combinatorial]:");
        System.out.println("                              * token:         Mutates whole tokens as cohesive units.");
        System.out.println("                              * single:        Sliding window mutating 1 character at a time across all indices.");
        System.out.println("                              * combinatorial: Multi-position subsets mutated with asymmetric transforms.");
        System.out.println("  --max-positions <n>         Max simultaneous positions in combinatorial mode (1-4, def: 2)");
        System.out.println("  --encoding-layers <n>       Max nested encoding layers for escape transforms (1-3, def: 1)");
        System.out.println("  -n, --max-permutations <n>  Maximum unique mutations to output (0 = Natural Exhaustion / uncapped, def: 150)");
        System.out.println("  --max-depth <n>             Max recursive transformation depth for chained mutations (default: 2)");
        System.out.println();
        System.out.println("Boundary Flags:");
        System.out.println("  --allow-non-printable       Allow unescaped C0/C1 control codes (0x00-0x1F, 0x7F) and lone surrogates");
        System.out.println("  --allow-null-bytes          Allow raw unescaped NUL (\\0) bytes in generated output");
        System.out.println("  --no-canonicalize           Disable auto-canonicalization of pre-encoded inputs");
        System.out.println("  --no-preserve-structure     Allow mutations to break semantic delimiter boundaries");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # 1. Whitelist Auditing (test strict ASCII alphanumeric boundary with 0 or provided input):");
        System.out.println("  java -jar input-mutator.jar -t \"admin\" -i whitelist -c alphanumeric");
        System.out.println();
        System.out.println("  # 2. Blacklist Evasion with specific categories:");
        System.out.println("  java -jar input-mutator.jar -t \"../secret\" -i blacklist --categories unicode,url,overlong");
        System.out.println();
        System.out.println("  # 3. Parser Differential Analysis (generate encoding desync payloads):");
        System.out.println("  java -jar input-mutator.jar -t \"id=1\" -i differential -n 50");
        System.out.println();
        System.out.println("  # 4. Zero-input archetype generation for email validation testing:");
        System.out.println("  java -jar input-mutator.jar --type email -n 30");
        System.out.println();
        System.out.println("  # 5. Uncapped Natural Exhaustion (pipe entire mathematical mutation space to wordlist):");
        System.out.println("  java -jar input-mutator.jar -t \"admin\" -i blacklist -n 0 > payloads.txt");
        System.out.println("================================================================================");
    }
}
