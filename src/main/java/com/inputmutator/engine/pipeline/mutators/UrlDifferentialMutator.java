package com.inputmutator.engine.pipeline.mutators;

import com.inputmutator.engine.model.InputType;
import com.inputmutator.engine.pipeline.Mutator;
import com.inputmutator.engine.pipeline.TransformationContext;
import com.inputmutator.engine.tokenizer.Token;
import com.inputmutator.engine.tokenizer.TokenType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates path matrix parameters, dot-segment normalization quirks, and proxy differential vectors.
 */
public class UrlDifferentialMutator implements Mutator {

    @Override
    public String name() {
        return "UrlDifferentialMutator";
    }

    @Override
    public boolean appliesTo(Token token, TransformationContext context) {
        InputType type = context.profile().inputType();
        if (type == InputType.URL_PATH) {
            return true;
        }
        String val = token.value();
        return val.contains("/") || val.contains("\\") || token.type() == TokenType.DELIMITER;
    }

    @Override
    public List<String> mutate(Token token, TransformationContext context) {
        String val = token.value();
        Set<String> results = new LinkedHashSet<>();

        if (val.equals("/")) {
            // Path matrix parameters (Spring / Tomcat semicolon stripping)
            results.add("/;param=1/");
            results.add("/;jsessionid=0/");
            // Alternate and redundant path delimiters
            results.add("//");
            results.add("/./");
            results.add("/..;/");
            results.add("/%2e%2e/");
            results.add("/%2f/");
            results.add("/\\");
        } else if (val.equals("\\")) {
            results.add("/");
            results.add("\\\\");
            results.add("\\..\\");
        } else {
            // Path segment variations
            results.add(val + ".");             // IIS trailing dot quirk
            results.add(val + ";");             // Trailing matrix delimiter
            results.add(val + ";param=1");      // Matrix parameter attachment
            results.add(val + "%20");           // URL encoded trailing space
            results.add(val + "/.");            // Dot-directory traversal
            results.add(val + "/..;/");         // Path traversal with matrix termination
            results.add("./" + val);            // Relative current-directory prefix
            results.add("%2e%2e/" + val);       // Encoded parent-directory prefix
        }

        results.remove(val);
        return new ArrayList<>(results);
    }
}
