package com.inputmutator.engine.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Operational intent guiding baseline artifact category selection and pipeline optimization.
 */
public enum TestingIntent {
    WHITELIST_AUDITING("Whitelist Auditing", "Conforms to strict character sets while testing downstream normalization expansion"),
    BLACKLIST_EVASION("Blacklist / Signature Evasion", "Obfuscates forbidden keywords and delimiters to evade signature filters"),
    PARSER_DIFFERENTIAL("Parser Differential Hunting", "Targets proxy/parser desync via delimiter ambiguity, matrix parameters, and comments");

    private final String displayName;
    private final String description;

    TestingIntent(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    /**
     * Default artifact categories recommended for this intent.
     */
    public Set<ArtifactCategory> defaultCategories() {
        return switch (this) {
            case WHITELIST_AUDITING -> EnumSet.of(
                    ArtifactCategory.UNICODE_HOMOGLYPHS,
                    ArtifactCategory.NUMERIC_RADIX
            );
            case BLACKLIST_EVASION -> EnumSet.of(
                    ArtifactCategory.URL_ENCODING,
                    ArtifactCategory.OVERLONG_UTF8,
                    ArtifactCategory.UNICODE_HOMOGLYPHS,
                    ArtifactCategory.HTML_ENTITIES,
                    ArtifactCategory.CONTROL_NON_PRINTABLE
            );
            case PARSER_DIFFERENTIAL -> EnumSet.of(
                    ArtifactCategory.GRAMMAR_DIFFERENTIAL,
                    ArtifactCategory.NUMERIC_RADIX,
                    ArtifactCategory.CONTROL_NON_PRINTABLE,
                    ArtifactCategory.URL_ENCODING
            );
        };
    }
}
