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
        StringBuilder urlEncUpper = new StringBuilder();
        StringBuilder urlEncLower = new StringBuilder();
        StringBuilder doubleEnc = new StringBuilder();
        byte[] bytes = val.getBytes(StandardCharsets.UTF_8);

        for (byte b : bytes) {
            String hexUpper = String.format("%02X", b);
            String hexLower = String.format("%02x", b);
            urlEncUpper.append("%").append(hexUpper);
            urlEncLower.append("%").append(hexLower);
            doubleEnc.append("%25").append(hexUpper);
        }
        results.add(urlEncUpper.toString());
        results.add(urlEncLower.toString());
        results.add(doubleEnc.toString());

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

        // 4. HTML Entities (for single characters / delimiters)
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
            results.add("&#x" + Integer.toHexString(cp) + ";");
            results.add("&#X" + Integer.toHexString(cp).toUpperCase() + ";");
        }

        // 5. JSON escaped slash
        if (val.contains("/")) {
            results.add(val.replace("/", "\\/"));
        }

        results.remove(val);
        return new ArrayList<>(results);
    }
}
