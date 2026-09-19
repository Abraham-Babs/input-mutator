package com.inputmutator.engine.pipeline.mutators;

import com.inputmutator.engine.pipeline.Mutator;
import com.inputmutator.engine.pipeline.TransformationContext;
import com.inputmutator.engine.tokenizer.Token;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies multi-context escaping and encoding (URL percent-encoding, HTML entities, JSON and C escapes).
 */
public class EscapingMutator implements Mutator {

    private static final Map<Character, String> HTML_NAMED = Map.of(
            '"', "&quot;",
            '\'', "&apos;",
            '<', "&lt;",
            '>', "&gt;",
            '&', "&amp;"
    );

    @Override
    public String name() {
        return "EscapingMutator";
    }

    @Override
    public boolean appliesTo(Token token, TransformationContext context) {
        return true; // Can attempt escape representation on any token
    }

    @Override
    public List<String> mutate(Token token, TransformationContext context) {
        String val = token.value();
        Set<String> results = new LinkedHashSet<>();

        // 1. URL Percent-Encoding (single, uppercase, lowercase)
        if (context.profile().isCategoryEnabled(com.inputmutator.engine.model.ArtifactCategory.URL_ENCODING)) {
            StringBuilder urlEncUpper = new StringBuilder();
            StringBuilder urlEncLower = new StringBuilder();
            StringBuilder doubleEnc = new StringBuilder();
            int layers = context.profile().encodingLayers();
            StringBuilder tripleEnc = new StringBuilder();
            byte[] bytes = val.getBytes(StandardCharsets.UTF_8);

            for (byte b : bytes) {
                String hexUpper = String.format("%02X", b);
                String hexLower = String.format("%02x", b);
                urlEncUpper.append("%").append(hexUpper);
                urlEncLower.append("%").append(hexLower);
                doubleEnc.append("%25").append(hexUpper);
                if (layers >= 3) {
                    tripleEnc.append("%2525").append(hexUpper);
                }
            }
            results.add(urlEncUpper.toString());
            results.add(urlEncLower.toString());
            if (layers >= 2) {
                results.add(doubleEnc.toString());
            }
            if (layers >= 3 && !tripleEnc.isEmpty()) {
                results.add(tripleEnc.toString());
            }
        }

        // 2. Unicode escape sequences (\\u followed by 4 hex digits or surrogate pairs)
        StringBuilder uniEscLower = new StringBuilder();
        StringBuilder uniEscUpper = new StringBuilder();
        val.codePoints().forEach(cp -> {
            if (cp <= 0xFFFF) {
                uniEscLower.append("\\").append(String.format("u%04x", cp));
                uniEscUpper.append("\\").append(String.format("u%04X", cp));
            } else {
                char[] surrogates = Character.toChars(cp);
                uniEscLower.append("\\").append(String.format("u%04x", (int) surrogates[0]))
                            .append("\\").append(String.format("u%04x", (int) surrogates[1]));
                uniEscUpper.append("\\").append(String.format("u%04X", (int) surrogates[0]))
                            .append("\\").append(String.format("u%04X", (int) surrogates[1]));
            }
        });
        results.add(uniEscLower.toString());
        results.add(uniEscUpper.toString());

        // 3. C-style hex escapes (\xXX) for ASCII
        boolean isAscii = true;
        StringBuilder hexEsc = new StringBuilder();
        for (int cp : val.codePoints().toArray()) {
            if (cp <= 0xFF) {
                hexEsc.append(String.format("\\x%02x", cp));
            } else {
                isAscii = false;
                break;
            }
        }
        if (isAscii && !hexEsc.isEmpty()) {
            results.add(hexEsc.toString());
        }

        // 3b. C-style octal escapes (\OOO) for ASCII
        StringBuilder octEsc = new StringBuilder();
        for (int cp : val.codePoints().toArray()) {
            if (cp <= 0xFF) {
                octEsc.append(String.format("\\%03o", cp));
            } else {
                octEsc.setLength(0);
                break;
            }
        }
        if (!octEsc.isEmpty()) {
            results.add(octEsc.toString());
        }

        // 4. HTML Entities (single characters and full tokens)
        if (context.profile().isCategoryEnabled(com.inputmutator.engine.model.ArtifactCategory.HTML_ENTITIES)) {
            if (val.codePointCount(0, val.length()) == 1) {
                int cp = val.codePointAt(0);
                if (cp == '"' || cp == '\'' || cp == '<' || cp == '>' || cp == '&') {
                    String named = HTML_NAMED.get((char) cp);
                    if (named != null) {
                        results.add(named);
                    }
                }
                results.add("&#" + cp + ";");
                results.add("&#0000" + cp + ";");
                results.add(String.format("&#%08d;", cp)); // 8-digit overlong zero-padded decimal
                results.add("&#x" + Integer.toHexString(cp) + ";");
                results.add(String.format("&#x%08x;", cp)); // 8-digit overlong zero-padded hex
                results.add("&#X" + Integer.toHexString(cp).toUpperCase() + ";");
            } else if (val.length() <= 32) {
                StringBuilder htmlDec = new StringBuilder();
                StringBuilder htmlHex = new StringBuilder();
                for (int cp : val.codePoints().toArray()) {
                    htmlDec.append("&#").append(cp).append(";");
                    htmlHex.append("&#x").append(Integer.toHexString(cp)).append(";");
                }
                results.add(htmlDec.toString());
                results.add(htmlHex.toString());
            }
        }

        // 5. Slash alternatives and JSON escaped slash
        if (val.contains("/")) {
            results.add(val.replace("/", "\\/"));
            results.add(val.replace("/", "\u2044")); // Unicode Fraction Slash
            results.add(val.replace("/", "\u2215")); // Unicode Division Slash
        }

        // 6. Overlong UTF-8 encodings for ASCII tokens and delimiters
        if (context.profile().isCategoryEnabled(com.inputmutator.engine.model.ArtifactCategory.OVERLONG_UTF8)) {
            boolean isAllAscii = val.chars().allMatch(c -> c < 128);
            if (isAllAscii && !val.isEmpty() && val.length() <= 16) {
                StringBuilder overlong2 = new StringBuilder();
                StringBuilder overlong3 = new StringBuilder();
                for (char c : val.toCharArray()) {
                    int b = (int) c;
                    overlong2.append(String.format("%%%02X%%%02X", 0xC0 | (b >> 6), 0x80 | (b & 0x3F)));
                    overlong3.append(String.format("%%%02X%%%02X%%%02X", 0xE0, 0x80 | (b >> 6), 0x80 | (b & 0x3F)));
                }
                results.add(overlong2.toString().toLowerCase());
                results.add(overlong2.toString().toUpperCase());
                results.add(overlong3.toString().toLowerCase());
            }
        }

        results.remove(val);
        return new ArrayList<>(results);
    }
}
