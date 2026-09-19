package com.inputmutator.engine;

import com.inputmutator.engine.constraint.ConstraintProfile;
import com.inputmutator.engine.model.ArtifactCategory;
import com.inputmutator.engine.model.TestingIntent;
import com.inputmutator.engine.pipeline.Mutator;
import com.inputmutator.engine.pipeline.StrategyDispatcher;
import com.inputmutator.engine.pipeline.TransformationContext;
import com.inputmutator.engine.pipeline.mutators.EscapingMutator;
import com.inputmutator.engine.pipeline.mutators.NumericRadixMutator;
import com.inputmutator.engine.pipeline.mutators.UnicodeMutator;
import com.inputmutator.engine.tokenizer.Token;
import com.inputmutator.engine.tokenizer.TokenType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StrategyDispatcherTest {

    private StrategyDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new StrategyDispatcher();
    }

    @Test
    void testWhitelistAuditingDefaults() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .testingIntent(TestingIntent.WHITELIST_AUDITING)
                .build();

        assertTrue(profile.isCategoryEnabled(ArtifactCategory.UNICODE_HOMOGLYPHS));
        assertTrue(profile.isCategoryEnabled(ArtifactCategory.NUMERIC_RADIX));
        assertFalse(profile.isCategoryEnabled(ArtifactCategory.URL_ENCODING));
        assertFalse(profile.isCategoryEnabled(ArtifactCategory.OVERLONG_UTF8));
    }

    @Test
    void testBlacklistEvasionDefaults() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .testingIntent(TestingIntent.BLACKLIST_EVASION)
                .build();

        assertTrue(profile.isCategoryEnabled(ArtifactCategory.URL_ENCODING));
        assertTrue(profile.isCategoryEnabled(ArtifactCategory.OVERLONG_UTF8));
        assertTrue(profile.isCategoryEnabled(ArtifactCategory.UNICODE_HOMOGLYPHS));
    }

    @Test
    void testCustomCategoryFiltering() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .enabledCategories(EnumSet.of(ArtifactCategory.NUMERIC_RADIX))
                .build();

        Mutator numMutator = new NumericRadixMutator();
        Mutator uniMutator = new UnicodeMutator();
        Mutator escMutator = new EscapingMutator();

        assertTrue(dispatcher.isMutatorCategoryEnabled(numMutator, profile));
        assertFalse(dispatcher.isMutatorCategoryEnabled(uniMutator, profile));
        assertFalse(dispatcher.isMutatorCategoryEnabled(escMutator, profile));
    }

    @Test
    void testFilterApplicableMutators() {
        ConstraintProfile profile = ConstraintProfile.builder()
                .enabledCategories(EnumSet.of(ArtifactCategory.UNICODE_HOMOGLYPHS))
                .build();
        TransformationContext context = new TransformationContext(profile);

        List<Mutator> mutators = List.of(new UnicodeMutator(), new NumericRadixMutator());
        Token token = new Token(TokenType.LITERAL, "admin", 0, 5);

        List<Mutator> applicable = dispatcher.filterApplicableMutators(mutators, token, context);
        assertEquals(1, applicable.size());
        assertEquals("UnicodeMutator", applicable.get(0).name());
    }
}
