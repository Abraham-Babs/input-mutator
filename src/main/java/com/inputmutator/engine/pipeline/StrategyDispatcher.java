package com.inputmutator.engine.pipeline;

import com.inputmutator.engine.constraint.ConstraintProfile;
import com.inputmutator.engine.model.ArtifactCategory;
import com.inputmutator.engine.tokenizer.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * Filters and dispatches mutators based on the active testing intent and enabled artifact categories.
 */
public class StrategyDispatcher {

    public List<Mutator> filterApplicableMutators(List<Mutator> allMutators, Token token, TransformationContext context) {
        return filterApplicableMutators(allMutators, token, context, false);
    }

    public List<Mutator> filterApplicableMutators(List<Mutator> allMutators, Token token, TransformationContext context, boolean isCharacterLevel) {
        List<Mutator> applicable = new ArrayList<>();
        ConstraintProfile profile = context.profile();

        for (Mutator m : allMutators) {
            if (isCharacterLevel && !m.supportsCharacterLevel()) {
                continue;
            }
            if (isMutatorCategoryEnabled(m, profile) && m.appliesTo(token, context)) {
                applicable.add(m);
            }
        }
        return applicable;
    }

    public boolean isMutatorCategoryEnabled(Mutator m, ConstraintProfile profile) {
        if ("EscapingMutator".equals(m.name())) {
            return profile.isCategoryEnabled(ArtifactCategory.URL_ENCODING)
                    || profile.isCategoryEnabled(ArtifactCategory.OVERLONG_UTF8)
                    || profile.isCategoryEnabled(ArtifactCategory.HTML_ENTITIES);
        }
        return profile.isCategoryEnabled(m.category());
    }
}
