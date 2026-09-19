package com.inputmutator.engine.pipeline.mutators;

import com.inputmutator.engine.pipeline.Mutator;
import com.inputmutator.engine.pipeline.TransformationContext;
import com.inputmutator.engine.tokenizer.Token;
import com.inputmutator.engine.tokenizer.TokenType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates JSON-specific parser differentials including Unicode-escaped keys, case variations, and numeric representation.
 */
public class JsonDifferentialMutator implements Mutator {

    @Override
    public String name() {
        return "JsonDifferentialMutator";
    }

    @Override
    public boolean supportsCharacterLevel() {
        return false;
    }

    @Override
    public boolean appliesTo(Token token, TransformationContext context) {
        return token.type() == TokenType.JSON_KEY || token.type() == TokenType.JSON_VALUE;
    }

    @Override
    public List<String> mutate(Token token, TransformationContext context) {
        String val = token.value();
        Set<String> results = new LinkedHashSet<>();

        if (token.type() == TokenType.JSON_KEY) {
            boolean quoted = val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2;
            String inner = quoted ? val.substring(1, val.length() - 1) : val;

            if (!inner.isEmpty()) {
                // 1. Unicode-escaped first character in JSON key (e.g. "\u0061dmin")
                int firstCp = inner.codePointAt(0);
                String firstCharEscaped = String.format("\\u%04x", firstCp);
                String rest = inner.substring(Character.charCount(firstCp));
                results.add("\"" + firstCharEscaped + rest + "\"");

                // 2. Fully Unicode-escaped key
                StringBuilder fullEscaped = new StringBuilder("\"");
                inner.codePoints().forEach(cp -> fullEscaped.append(String.format("\\u%04x", cp)));
                fullEscaped.append("\"");
                results.add(fullEscaped.toString());

                // 3. Trailing / leading whitespace inside key
                results.add("\"" + inner + " \"");
                results.add("\" " + inner + "\"");

                // 4. Case-variation keys for case-tolerant parsers
                results.add("\"" + inner.toUpperCase() + "\"");
            }
        } else if (token.type() == TokenType.JSON_VALUE) {
            if (val.matches("^-?\\d+(\\.\\d+)?$")) {
                // Numeric values inside JSON
                results.add("1e999");            // Infinity overflow
                results.add("1e-324");           // Subnormal underflow
                results.add(val + ".0");
                results.add(val + "e0");
            } else if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                String inner = val.substring(1, val.length() - 1);
                // Unicode-escaped string value
                if (!inner.isEmpty()) {
                    int cp = inner.codePointAt(0);
                    results.add("\"" + String.format("\\u%04x", cp) + inner.substring(Character.charCount(cp)) + "\"");
                }
            }
        }

        results.remove(val);
        return new ArrayList<>(results);
    }
}
