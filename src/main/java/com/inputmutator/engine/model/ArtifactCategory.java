package com.inputmutator.engine.model;

/**
 * Artifact vector categories that can be toggled by the user or preset by intent.
 */
public enum ArtifactCategory {
    URL_ENCODING("URL Encodings (%xx, %25xx, triple)"),
    OVERLONG_UTF8("Overlong UTF-8 (%c0%af, %e0%80%af)"),
    UNICODE_HOMOGLYPHS("Unicode & Homoglyphs (NFKC, Cyrillic, Fullwidth)"),
    HTML_ENTITIES("HTML Entities (Named, Decimal, Hex)"),
    NUMERIC_RADIX("Numeric Radices & IEEE Specials (Hex, Octal, NaN)"),
    GRAMMAR_DIFFERENTIAL("Grammar & Parser Differentials (Matrix, Slashes, Delimiters)"),
    CONTROL_NON_PRINTABLE("Controls & Non-Printables (Nulls, Zero-Width)");

    private final String description;

    ArtifactCategory(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
