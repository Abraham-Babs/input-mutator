package com.inputmutator.burp;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.intruder.AttackConfiguration;
import burp.api.montoya.intruder.GeneratedPayload;
import burp.api.montoya.intruder.IntruderInsertionPoint;
import burp.api.montoya.intruder.PayloadGenerator;
import burp.api.montoya.intruder.PayloadGeneratorProvider;
import com.inputmutator.engine.constraint.ConstraintProfile;
import com.inputmutator.engine.pipeline.MutationEngine;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

/**
 * Registers Input Mutator as a custom automated payload generator in Burp Intruder.
 */
public class MutatorPayloadGeneratorProvider implements PayloadGeneratorProvider {

    private final MutationEngine engine;

    public MutatorPayloadGeneratorProvider() {
        this.engine = new MutationEngine();
    }

    @Override
    public String displayName() {
        return "Input Mutator - Constraint Engine";
    }

    @Override
    public PayloadGenerator providePayloadGenerator(AttackConfiguration attackConfiguration) {
        return new PayloadGenerator() {
            private Queue<String> payloadQueue = null;

            @Override
            public GeneratedPayload generatePayloadFor(IntruderInsertionPoint insertionPoint) {
                if (payloadQueue == null) {
                    String baseStr = "admin";
                    if (insertionPoint != null && insertionPoint.baseValue() != null && insertionPoint.baseValue().length() > 0) {
                        baseStr = insertionPoint.baseValue().toString();
                    }
                    ConstraintProfile profile = ConstraintProfile.builder()
                            .maxPermutations(100)
                            .maxDepth(2)
                            .build();

                    List<String> results = engine.generate(baseStr, profile);
                    payloadQueue = new ArrayDeque<>(results);
                }

                if (payloadQueue.isEmpty()) {
                    return GeneratedPayload.end();
                }

                String nextPayload = payloadQueue.poll();
                return GeneratedPayload.payload(ByteArray.byteArray(nextPayload));
            }
        };
    }
}
