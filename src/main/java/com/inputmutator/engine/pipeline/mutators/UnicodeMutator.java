package com.inputmutator.engine.pipeline.mutators;

import com.inputmutator.engine.pipeline.Mutator;
import com.inputmutator.engine.pipeline.TransformationContext;
import com.inputmutator.engine.tokenizer.Token;
import com.inputmutator.engine.tokenizer.TokenType;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mutates tokens into Unicode normalization variants, homoglyphs, ligatures, and full-width forms.
 */
public class UnicodeMutator implements Mutator {

    private static final Map<Character, String> FULL_WIDTH_MAP = Map.ofEntries(
            Map.entry('<', "\uFF1C"),
            Map.entry('>', "\uFF1E"),
            Map.entry('/', "\uFF0F"),
            Map.entry('\\', "\uFF3C"),
            Map.entry('\'', "\uFF07"),
            Map.entry('"', "\uFF02"),
            Map.entry('=', "\uFF1D"),
            Map.entry('&', "\uFF06"),
            Map.entry('.', "\uFF0E"),
            Map.entry(':', "\uFF1A"),
            Map.entry(';', "\uFF1B"),
            Map.entry('-', "\uFF0D"),
            Map.entry('_', "\uFF3F")
    );

    private static final Map<Character, Character> HOMOGLYPH_MAP = Map.ofEntries(
            Map.entry('a', '\u0430'), // Cyrillic small letter a
            Map.entry('e', '\u0435'), // Cyrillic small letter ie
            Map.entry('i', '\u0456'), // Cyrillic small letter byelorussian-ukrainian i
            Map.entry('j', '\u0458'), // Cyrillic small letter je
            Map.entry('o', '\u043E'), // Cyrillic small letter o
            Map.entry('p', '\u0440'), // Cyrillic small letter er
            Map.entry('c', '\u0441'), // Cyrillic small letter es
            Map.entry('s', '\u0455'), // Cyrillic small letter dze
            Map.entry('x', '\u0445'), // Cyrillic small letter ha
            Map.entry('y', '\u0443')  // Cyrillic small letter u
    );

    private static final Map<String, String> LIGATURE_MAP = Map.of(
            "ff", "\uFB00",
            "fi", "\uFB01",
            "fl", "\uFB02",
            "ffi", "\uFB03",
            "ffl", "\uFB04",
            "ij", "\u0133"
    );

    private static final Map<Character, String> NFKC_SINGLETON_MAP = Map.of(
            'k', "\u212A", // Kelvin sign normalizes to 'k'/'K'
            'K', "\u212A",
            'a', "\u00AA", // Feminine ordinal indicator normalizes to 'a'
            'o', "\u00BA", // Masculine ordinal indicator normalizes to 'o'
            's', "\u017F"  // Latin small letter long s normalizes to 's'
    );

    private static final Map<Character, String> GREEK_CONFUSABLE_MAP = Map.of(
            'o', "\u03BF", // Greek small letter omicron
            'v', "\u03BD", // Greek small letter nu
            'p', "\u03C1", // Greek small letter rho
            'x', "\u03C7", // Greek small letter chi
            'a', "\u03B1", // Greek small letter alpha
            'e', "\u03B5", // Greek small letter epsilon
            'i', "\u03B9"  // Greek small letter iota
    );

    private static final Map<Character, String> TURKISH_CASE_MAP = Map.of(
            'i', "\u0131", // Turkish dotless small i
            'I', "\u0130"  // Turkish dotted capital I
    );

    @Override
    public String name() {
        return "UnicodeMutator";
    }

    @Override
    public com.inputmutator.engine.model.ArtifactCategory category() {
        return com.inputmutator.engine.model.ArtifactCategory.UNICODE_HOMOGLYPHS;
    }

    @Override
    public boolean appliesTo(Token token, TransformationContext context) {
        TokenType t = token.type();
        return t == TokenType.LITERAL || t == TokenType.DELIMITER ||
               t == TokenType.SPECIAL_CHAR || t == TokenType.EMAIL_LOCAL ||
               t == TokenType.JSON_VALUE || t == TokenType.JSON_KEY;
    }

    @Override
    public List<String> mutate(Token token, TransformationContext context) {
        String val = token.value();
        Set<String> results = new LinkedHashSet<>();

        // 1. Normalization forms
        String nfd = Normalizer.normalize(val, Normalizer.Form.NFD);
        if (!nfd.equals(val)) results.add(nfd);

        String nfc = Normalizer.normalize(val, Normalizer.Form.NFC);
        if (!nfc.equals(val)) results.add(nfc);

        String nfkd = Normalizer.normalize(val, Normalizer.Form.NFKD);
        if (!nfkd.equals(val)) results.add(nfkd);

        String nfkc = Normalizer.normalize(val, Normalizer.Form.NFKC);
        if (!nfkc.equals(val)) results.add(nfkc);

        // 2. Full-width conversion
        StringBuilder fw = new StringBuilder();
        boolean hasFw = false;
        for (int cp : val.codePoints().toArray()) {
            if (cp <= 0xFFFF) {
                char c = (char) cp;
                String mapped = FULL_WIDTH_MAP.get(c);
                if (mapped != null) {
                    fw.append(mapped);
                    hasFw = true;
                } else if (c >= 0x21 && c <= 0x7E) {
                    fw.append((char) (c + 0xFEE0));
                    hasFw = true;
                } else {
                    fw.append(c);
                }
            } else {
                fw.append(Character.toChars(cp));
            }
        }
        if (hasFw) {
            results.add(fw.toString());
        }

        // 3. Homoglyphs
        StringBuilder hg = new StringBuilder();
        boolean hasHg = false;
        for (int cp : val.codePoints().toArray()) {
            if (cp <= 0xFFFF) {
                char c = (char) cp;
                Character mapped = HOMOGLYPH_MAP.get(c);
                if (mapped != null) {
                    hg.append(mapped);
                    hasHg = true;
                } else {
                    hg.append(c);
                }
            } else {
                hg.append(Character.toChars(cp));
            }
        }
        if (hasHg) {
            results.add(hg.toString());
        }

        // 4. Ligature substitution
        for (Map.Entry<String, String> entry : LIGATURE_MAP.entrySet()) {
            if (val.contains(entry.getKey())) {
                results.add(val.replace(entry.getKey(), entry.getValue()));
            }
        }

        // 4b. NFKC singleton compatibility substitutions
        for (Map.Entry<Character, String> entry : NFKC_SINGLETON_MAP.entrySet()) {
            char target = entry.getKey();
            if (val.indexOf(target) >= 0) {
                results.add(val.replace(String.valueOf(target), entry.getValue()));
            }
        }

        // 4c. Greek confusable substitutions
        for (Map.Entry<Character, String> entry : GREEK_CONFUSABLE_MAP.entrySet()) {
            char target = entry.getKey();
            if (val.indexOf(target) >= 0) {
                results.add(val.replace(String.valueOf(target), entry.getValue()));
            }
        }

        // 4d. Turkish locale-sensitive case variants
        for (Map.Entry<Character, String> entry : TURKISH_CASE_MAP.entrySet()) {
            char target = entry.getKey();
            if (val.indexOf(target) >= 0) {
                results.add(val.replace(String.valueOf(target), entry.getValue()));
            }
        }

        // 4e. Collation and regex splitters (soft hyphen and zero-width non-joiner)
        if (val.length() > 1) {
            results.add(val.charAt(0) + "\u00AD" + val.substring(1));
            results.add(val.charAt(0) + "\u200C" + val.substring(1));
            results.add(val.charAt(0) + "\u200B" + val.substring(1)); // Zero-Width Space
            results.add(val.charAt(0) + "\u2060" + val.substring(1)); // Word Joiner
        }

        // 4f. Circled / enclosed alphanumerics
        String circled = toCircled(val);
        if (circled != null) {
            results.add(circled);
        }

        // 5. Combining diacritical marks (inject after first character)
        if (!val.isEmpty()) {
            results.add(val.charAt(0) + "\u0301" + (val.length() > 1 ? val.substring(1) : ""));
            results.add(val.charAt(0) + "\u0300" + (val.length() > 1 ? val.substring(1) : ""));
        }

        // 6. Mathematical Alphanumeric Symbols (Plane 1 surrogate pairs e.g. 𝐚, 𝐛, 𝕒)
        StringBuilder mathBold = new StringBuilder();
        StringBuilder mathMono = new StringBuilder();
        boolean hasMath = false;

        for (int cp : val.codePoints().toArray()) {
            if (cp >= 'a' && cp <= 'z') {
                mathBold.append(Character.toChars(0x1D41A + (cp - 'a')));
                mathMono.append(Character.toChars(0x1D68A + (cp - 'a')));
                hasMath = true;
            } else if (cp >= 'A' && cp <= 'Z') {
                mathBold.append(Character.toChars(0x1D400 + (cp - 'A')));
                mathMono.append(Character.toChars(0x1D670 + (cp - 'A')));
                hasMath = true;
            } else if (cp >= '0' && cp <= '9') {
                mathBold.append(Character.toChars(0x1D7CE + (cp - '0')));
                mathMono.append(Character.toChars(0x1D7F6 + (cp - '0')));
                hasMath = true;
            } else {
                mathBold.append(Character.toChars(cp));
                mathMono.append(Character.toChars(cp));
            }
        }
        if (hasMath) {
            results.add(mathBold.toString());
            results.add(mathMono.toString());
        }

        // 7. Isolated / Unpaired surrogates (fuzzing UTF-16 transcoders)
        if (context.profile().allowNonPrintable()) {
            results.add("\uD800" + val); // Lone high surrogate
            results.add(val + "\uDC00"); // Lone low surrogate
        }

        return new ArrayList<>(results);
    }

    private String toCircled(String s) {
        StringBuilder sb = new StringBuilder();
        boolean hasCircled = false;
        for (char c : s.toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                sb.append((char) ('\u24D0' + (c - 'a')));
                hasCircled = true;
            } else if (c >= 'A' && c <= 'Z') {
                sb.append((char) ('\u24B6' + (c - 'A')));
                hasCircled = true;
            } else if (c >= '1' && c <= '9') {
                sb.append((char) ('\u2460' + (c - '1')));
                hasCircled = true;
            } else {
                sb.append(c);
            }
        }
        return hasCircled ? sb.toString() : null;
    }
}
