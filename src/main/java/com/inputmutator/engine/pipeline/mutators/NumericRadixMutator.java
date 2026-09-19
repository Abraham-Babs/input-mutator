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
 * Mutates numeric tokens into alternate radices, scientific notation, leading zeros, and boundary conditions.
 */
public class NumericRadixMutator implements Mutator {

    @Override
    public String name() {
        return "NumericRadixMutator";
    }

    @Override
    public boolean supportsCharacterLevel() {
        return false;
    }

    @Override
    public com.inputmutator.engine.model.ArtifactCategory category() {
        return com.inputmutator.engine.model.ArtifactCategory.NUMERIC_RADIX;
    }

    @Override
    public boolean appliesTo(Token token, TransformationContext context) {
        return token.type() == TokenType.NUMERIC;
    }

    @Override
    public List<String> mutate(Token token, TransformationContext context) {
        String val = token.value().trim();
        Set<String> results = new LinkedHashSet<>();

        try {
            // Parse long or double
            if (val.contains(".") || val.toLowerCase().contains("e")) {
                double d = Double.parseDouble(val);
                results.add(String.format("%e", d));
                results.add(String.format("%E", d));
                results.add("+" + val);
                results.add("-0.0");
                results.add("NaN");
                results.add("Infinity");
                results.add("-Infinity");
                if (val.startsWith("0.")) {
                    results.add(val.substring(1)); // e.g. .5
                }
            } else {
                long num;
                if (val.startsWith("0x") || val.startsWith("0X")) {
                    num = Long.parseLong(val.substring(2), 16);
                } else if (val.startsWith("0b") || val.startsWith("0B")) {
                    num = Long.parseLong(val.substring(2), 2);
                } else if (val.startsWith("0o") || val.startsWith("0O")) {
                    num = Long.parseLong(val.substring(2), 8);
                } else {
                    num = Long.parseLong(val);
                }

                // Radix conversions
                results.add("0x" + Long.toHexString(num));
                results.add("0X" + Long.toHexString(num).toUpperCase());
                results.add("0" + Long.toOctalString(num));
                results.add("0o" + Long.toOctalString(num));
                results.add("0b" + Long.toBinaryString(num));

                // Padding & signs
                results.add(String.format("%08d", num));
                results.add("+" + num);

                // Scientific notation
                results.add(num + "e0");
                results.add(num + "E0");

                // Boundary values
                results.add("0");
                results.add("-1");
                results.add(String.valueOf(Integer.MAX_VALUE));
                results.add(String.valueOf((long) Integer.MAX_VALUE + 1)); // 32-bit signed overflow
                results.add(String.valueOf(Long.MAX_VALUE));
            }

            // Unicode cultural & fullwidth digit representations
            results.add(toFullwidthDigits(val));
            results.add(toArabicIndicDigits(val));
        } catch (NumberFormatException ignored) {
            // Not directly parseable as standard number, keep graceful
        }

        results.remove(val);
        return new ArrayList<>(results);
    }

    private String toFullwidthDigits(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c >= '0' && c <= '9') {
                sb.append((char) (c - '0' + 0xFF10));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String toArabicIndicDigits(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c >= '0' && c <= '9') {
                sb.append((char) (c - '0' + 0x0660));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
