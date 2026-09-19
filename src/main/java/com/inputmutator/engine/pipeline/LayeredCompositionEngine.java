package com.inputmutator.engine.pipeline;

import com.inputmutator.engine.model.ArtifactCategory;
import com.inputmutator.engine.tokenizer.Token;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Orchestrates principled 3-step cross-artifact layering:
 * Step 1: Character / Structural Mutation (Homoglyph, Radix, Boundary, Delimiter)
 * Step 2: Representation / Wire Encoding (HTML Entity, Unicode escape, URL percent-encoding)
 */
public class LayeredCompositionEngine {

    public List<String> composeLayers(Token token, TransformationContext context, List<Mutator> mutators) {
        if (context.profile().encodingLayers() < 2) {
            return List.of();
        }

        Set<String> compositeResults = new LinkedHashSet<>();

        // Step 1: Gather primary character/structural variants
        List<String> primaryVariants = new ArrayList<>();
        for (Mutator m : mutators) {
            if (m.category() == ArtifactCategory.UNICODE_HOMOGLYPHS ||
                m.category() == ArtifactCategory.NUMERIC_RADIX ||
                m.category() == ArtifactCategory.GRAMMAR_DIFFERENTIAL) {
                if (m.appliesTo(token, context)) {
                    List<String> vars = m.mutate(token, context);
                    for (String v : vars) {
                        if (!v.equals(token.value())) {
                            primaryVariants.add(v);
                            if (primaryVariants.size() >= 12) break;
                        }
                    }
                }
            }
        }

        // Step 2 & 3: Package primary variants into wire encodings (URL %-encoding or escapes)
        Mutator escapingMutator = mutators.stream()
                .filter(m -> m.name().equals("EscapingMutator"))
                .findFirst().orElse(null);

        if (escapingMutator != null) {
            for (String primary : primaryVariants) {
                Token virtualToken = new Token(token.type(), primary, token.startIndex(), token.startIndex() + primary.length());
                if (escapingMutator.appliesTo(virtualToken, context)) {
                    List<String> encodedVariants = escapingMutator.mutate(virtualToken, context);
                    for (String ev : encodedVariants) {
                        if (!ev.equals(primary) && !ev.equals(token.value())) {
                            compositeResults.add(ev);
                            if (compositeResults.size() >= 30) break;
                        }
                    }
                }
            }
        }

        return new ArrayList<>(compositeResults);
    }
}
