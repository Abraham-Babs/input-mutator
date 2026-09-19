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

    private static final int SAFE_INTRUDER_MAX_PERMUTATIONS = 2500;
    private final MutationEngine engine;
    private final java.util.function.Supplier<ConstraintProfile> profileSupplier;

    public MutatorPayloadGeneratorProvider() {
        this((java.util.function.Supplier<ConstraintProfile>) null);
    }

    public MutatorPayloadGeneratorProvider(com.inputmutator.ui.MainPanel mainPanel) {
        this(mainPanel != null ? mainPanel::getActiveProfile : null);
    }

    public MutatorPayloadGeneratorProvider(java.util.function.Supplier<ConstraintProfile> profileSupplier) {
        this.engine = new MutationEngine();
        this.profileSupplier = profileSupplier;
    }

    @Override
    public String displayName() {
        return "Input Mutator - Constraint Engine";
    }

    @Override
    public PayloadGenerator providePayloadGenerator(AttackConfiguration attackConfiguration) {
        return new PayloadGenerator() {
            private final Object lock = new Object();
            private volatile java.util.concurrent.ConcurrentLinkedQueue<String> payloadQueue = null;

            @Override
            public GeneratedPayload generatePayloadFor(IntruderInsertionPoint insertionPoint) {
                if (payloadQueue == null) {
                    synchronized (lock) {
                        if (payloadQueue == null) {
                            String baseStr = "admin";
                            if (insertionPoint != null && insertionPoint.baseValue() != null && insertionPoint.baseValue().length() > 0) {
                                baseStr = insertionPoint.baseValue().toString();
                            }
                            ConstraintProfile active = (profileSupplier != null) ? profileSupplier.get() : null;
                            ConstraintProfile profile;
                            if (active != null) {
                                int limit = active.isUncapped() ? SAFE_INTRUDER_MAX_PERMUTATIONS : active.maxPermutations();
                                profile = active.toBuilder().maxPermutations(limit).build();
                            } else {
                                profile = ConstraintProfile.builder()
                                        .maxPermutations(SAFE_INTRUDER_MAX_PERMUTATIONS)
                                        .maxDepth(2)
                                        .build();
                            }

                            List<String> results = engine.generate(baseStr, profile);
                            payloadQueue = new java.util.concurrent.ConcurrentLinkedQueue<>(results);
                        }
                    }
                }

                String nextPayload = payloadQueue.poll();
                if (nextPayload == null) {
                    return GeneratedPayload.end();
                }
                return GeneratedPayload.payload(ByteArray.byteArray(nextPayload));
            }
        };
    }
}
