package com.kalessil.phpStorm.phpInspectionsEA.inspectors.languageConstructions;


import com.intellij.codeInsight.PsiEquivalenceUtil;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiWhiteSpace;
import com.jetbrains.php.lang.psi.elements.TernaryExpression;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpElementVisitor;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpInspection;
import com.kalessil.phpStorm.phpInspectionsEA.utils.ExpressionSemanticUtil;
import org.jetbrains.annotations.NotNull;

public class ElvisOperatorCanBeUsedInspector extends BasePhpInspection {
    private static final String strProblemDescription = "' ... ?: ...' construction shall be used instead";

    @NotNull
    public String getShortName() {
        return "ElvisOperatorCanBeUsedInspection";
    }

    @Override
    @NotNull
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new BasePhpElementVisitor() {
            public void visitPhpTernaryExpression(TernaryExpression expression) {
                /** construction requirements */
                final PsiElement objTrueVariant = ExpressionSemanticUtil.getExpressionTroughParenthesis(expression.getTrueVariant());
                if (null == objTrueVariant) {
                    return;
                }
                final PsiElement objCondition = ExpressionSemanticUtil.getExpressionTroughParenthesis(expression.getCondition());
                if (null == objCondition) {
                    return;
                }

                /** if true variant is the object or expressions are not equals */
                if (objCondition != objTrueVariant && PsiEquivalenceUtil.areElementsEquivalent(objCondition, objTrueVariant)) {
                    holder.registerProblem(expression.getTrueVariant(), strProblemDescription, ProblemHighlightType.WEAK_WARNING, new TheLocalFix());
                }
            }
        };
    }

    static private class TheLocalFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() {
            return "Use ?: instead";
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return getName();
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement target = descriptor.getPsiElement();

            /* cleanup spaces around */
            PsiElement before = target.getPrevSibling();
            if (before instanceof PsiWhiteSpace) {
                before.delete();
            }
            PsiElement after = target.getNextSibling();
            if (after instanceof PsiWhiteSpace) {
                after.delete();
            }

            /* drop true expression */
            target.delete();
        }
    }
}
