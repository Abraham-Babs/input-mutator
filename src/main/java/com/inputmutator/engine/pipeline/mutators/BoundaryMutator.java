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
 * Mutates tokens to target boundary conditions, off-by-one limits, and delimiter duplication.
 */
public class BoundaryMutator implements Mutator {

    @Override
    public String name() {
        return "BoundaryMutator";
    }

    @Override
    public boolean appliesTo(Token token, TransformationContext context) {
        return true;
    }

    @Override
    public List<String> mutate(Token token, TransformationContext context) {
        String val = token.value();
        Set<String> results = new LinkedHashSet<>();

        // 1. Delimiter duplication
        if (token.type() == TokenType.DELIMITER) {
            results.add(val + val); // e.g. //, '', ""
            results.add(val + val + val);
        }

        // 2. Trailing dot / slash variations
        if (val.equals("/")) {
            results.add("/.");
            results.add("/./");
            results.add("/../");
        } else if (val.equals(".")) {
            results.add("..");
            results.add("...");
        }

        // 3. Length boundary extension toward profile maxLength
        int maxLen = context.profile().maxLength();
        if (val.length() < maxLen && val.length() > 0 && token.type() == TokenType.LITERAL) {
            char padChar = val.charAt(0);
            int targetPad = Math.min(maxLen - val.length(), 16);
            if (targetPad > 0) {
                results.add(val + String.valueOf(padChar).repeat(targetPad));
            }
        }

        // 4. Truncation boundary
        if (val.length() > 1) {
            results.add(val.substring(0, val.length() - 1));
        }

        results.remove(val);
        return new ArrayList<>(results);
    }
}
