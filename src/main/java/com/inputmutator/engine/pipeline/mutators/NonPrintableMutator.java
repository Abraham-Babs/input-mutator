package com.inputmutator.engine.pipeline.mutators;

import com.inputmutator.engine.pipeline.Mutator;
import com.inputmutator.engine.pipeline.TransformationContext;
import com.inputmutator.engine.tokenizer.Token;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Injects non-printable, zero-width, and boundary control characters.
 */
public class NonPrintableMutator implements Mutator {

    private static final String[] ZERO_WIDTH_CHARS = {
            "\u200B", // Zero-Width Space
            "\u200C", // Zero-Width Non-Joiner
            "\u200D", // Zero-Width Joiner
            "\u2060", // Word Joiner
            "\uFEFF"  // Zero-Width No-Break Space / BOM
    };

    private static final String[] CONTROL_CODES = {
            "\0",     // NUL byte
            "\u0007", // BEL
            "\b",     // Backspace
            "\u000B", // Vertical Tab
            "\f"      // Form Feed
    };

    private static final String[] ESCAPED_NULS = {
            "%00",
            "\\0",
            "\\x00",
            "\\u0000"
    };

    @Override
    public String name() {
        return "NonPrintableMutator";
    }

    @Override
    public boolean appliesTo(Token token, TransformationContext context) {
        return !token.value().isEmpty();
    }

    @Override
    public List<String> mutate(Token token, TransformationContext context) {
        String val = token.value();
        Set<String> results = new LinkedHashSet<>();
        boolean allowControls = context.profile().allowNonPrintable();
        boolean allowNulls = context.profile().allowNullBytes();

        // 1. Zero-width character insertions (middle and boundary)
        for (String zw : ZERO_WIDTH_CHARS) {
            results.add(zw + val);
            results.add(val + zw);
            if (val.length() > 1) {
                int mid = val.length() / 2;
                results.add(val.substring(0, mid) + zw + val.substring(mid));
            }
        }

        // 2. Control codes injection (only if allowed by profile)
        if (allowControls) {
            for (String ctrl : CONTROL_CODES) {
                if (ctrl.equals("\0") && !allowNulls) {
                    continue;
                }
                results.add(val + ctrl);
                results.add(ctrl + val);
            }
        }

        // 3. Encoded null representation (safe from unescaped control filters)
        for (String escNul : ESCAPED_NULS) {
            results.add(val + escNul);
            results.add(escNul + val);
            if (val.length() > 1) {
                int mid = val.length() / 2;
                results.add(val.substring(0, mid) + escNul + val.substring(mid));
            }
        }

        // 4. Whitespace alternatives (Non-Breaking Space, En-space, Em-space)
        if (val.equals(" ")) {
            results.add("\u00A0"); // NBSP
            results.add("\u2002"); // En-space
            results.add("\u2003"); // Em-space
            results.add("\u2009"); // Thin space
            results.add("\t");
            results.add("\r\n");
        }

        results.remove(val);
        return new ArrayList<>(results);
    }
}
