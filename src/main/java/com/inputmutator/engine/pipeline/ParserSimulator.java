package com.inputmutator.engine.pipeline;

import com.inputmutator.engine.encoding.Canonicalizer;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

/**
 * Simulates how candidate mutated inputs resolve under downstream parser normalization layers.
 */
public class ParserSimulator {

    private final Canonicalizer canonicalizer;

    public record SimulationResult(
            String raw,
            String urlDecoded,
            String nfkcNormalized,
            String htmlUnescaped,
            String fullyCanonicalized,
            boolean matchesOriginal
    ) {}

    public ParserSimulator() {
        this.canonicalizer = new Canonicalizer();
    }

    public SimulationResult simulate(String mutation, String originalInput) {
        if (mutation == null) {
            return new SimulationResult("", "", "", "", "", false);
        }

        String urlDecoded;
        try {
            urlDecoded = URLDecoder.decode(mutation, StandardCharsets.UTF_8);
        } catch (Exception e) {
            urlDecoded = mutation;
        }

        String nfkc = Normalizer.normalize(mutation, Normalizer.Form.NFKC);

        String htmlUnescaped = canonicalizer.decodeHtmlEntities(mutation);

        String full = canonicalizer.fullCanonicalize(mutation);

        boolean matches = originalInput != null && !originalInput.isBlank()
                && (full.equalsIgnoreCase(canonicalizer.fullCanonicalize(originalInput)) ||
                    urlDecoded.equalsIgnoreCase(originalInput) ||
                    nfkc.equalsIgnoreCase(originalInput));

        return new SimulationResult(
                mutation,
                urlDecoded,
                nfkc,
                htmlUnescaped,
                full,
                matches
        );
    }
}
