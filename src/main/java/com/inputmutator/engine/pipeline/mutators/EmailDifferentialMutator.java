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
 * Generates RFC 5322 comment wrapping, quoted identifier escapes, and sub-addressing differentials.
 */
public class EmailDifferentialMutator implements Mutator {

    @Override
    public String name() {
        return "EmailDifferentialMutator";
    }

    @Override
    public boolean supportsCharacterLevel() {
        return false;
    }

    @Override
    public boolean appliesTo(Token token, TransformationContext context) {
        return token.type() == TokenType.EMAIL_LOCAL;
    }

    @Override
    public List<String> mutate(Token token, TransformationContext context) {
        String val = token.value();
        Set<String> results = new LinkedHashSet<>();

        // 1. Sub-addressing / tag injection
        results.add(val + "+tag");
        results.add(val + "+test");

        // 2. RFC 5322 parenthetical comments (stripped by standard MTAs)
        results.add(val + "(test)");
        results.add("(comment)" + val);

        // 3. Quoted-string local parts allowing special characters
        if (!val.startsWith("\"")) {
            results.add("\"" + val + "\"");
            results.add("\"" + val + " \"");
            results.add("\"" + val + "@escaped\"");
        }

        // 4. Dot variations in local-part
        if (val.length() > 2 && !val.contains(".")) {
            results.add(val.charAt(0) + "." + val.substring(1));
            results.add(val.substring(0, val.length() - 1) + "." + val.charAt(val.length() - 1));
        }

        results.remove(val);
        return new ArrayList<>(results);
    }
}
