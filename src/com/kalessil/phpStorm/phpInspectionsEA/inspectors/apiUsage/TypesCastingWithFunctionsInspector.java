package com.kalessil.phpStorm.phpInspectionsEA.inspectors.apiUsage;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.BinaryExpression;
import com.jetbrains.php.lang.psi.elements.FunctionReference;
import com.jetbrains.php.lang.psi.elements.TernaryExpression;
import com.jetbrains.php.lang.psi.elements.UnaryExpression;
import com.jetbrains.php.lang.psi.elements.impl.ParenthesizedExpressionImpl;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpElementVisitor;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpInspection;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class TypesCastingWithFunctionsInspector extends BasePhpInspection {
    private static final String strProblemDescription = "'(%s) ...' construction shall be used instead";

    @NotNull
    public String getShortName() {
        return "TypesCastingWithFunctionsInspection";
    }

    private static final HashMap<String, String> mapping = new HashMap<String, String>();
    static {
        mapping.put("intval",   "int");
        mapping.put("floatval", "float");
        mapping.put("strval",   "string");
        mapping.put("boolval",  "bool");
        mapping.put("settype",  "bool|float|int|string");
    }

    @Override
    @NotNull
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new BasePhpElementVisitor() {
            public void visitPhpFunctionCall(FunctionReference reference) {
                /* check construction requirements */
                final int intArgumentsCount = reference.getParameters().length;
                final String strFunction    = reference.getName();
                if (intArgumentsCount != 1 || StringUtil.isEmpty(strFunction)) {
                    return;
                }

                /* check if inspection subject*/
                if (mapping.containsKey(strFunction)) {
                    final String suggestedType = mapping.get(strFunction);
                    final String strWarning    = strProblemDescription.replace("%s", suggestedType);

                    if (strFunction.equals("settype")) {
                        holder.registerProblem(reference, strWarning, ProblemHighlightType.LIKE_DEPRECATED);
                    } else {
                        holder.registerProblem(reference, strWarning, ProblemHighlightType.LIKE_DEPRECATED, new TheLocalFix(suggestedType));
                    }
                }
            }
        };
    }

    private static class TheLocalFix implements LocalQuickFix {
        final private String suggestedType;

        TheLocalFix(@NotNull String suggestedType) {
            super();
            this.suggestedType = suggestedType;
        }

        @NotNull
        @Override
        public String getName() {
            return "Use type casting";
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return getName();
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            final PsiElement expression = descriptor.getPsiElement();
            if (expression instanceof FunctionReference) {
                PsiElement parameter = ((FunctionReference) expression).getParameters()[0];
                if (parameter instanceof BinaryExpression || parameter instanceof UnaryExpression || parameter instanceof TernaryExpression) {
                    final String castingParameter  = "(" + parameter.getText() + ")";
                    parameter = PhpPsiElementFactory.createFromText(project, ParenthesizedExpressionImpl.class, castingParameter);
                }

                final String castingPattern  = "(" + this.suggestedType + ") null";
                final PsiElement replacement = PhpPsiElementFactory.createFromText(project, UnaryExpression.class, castingPattern);
                //noinspection ConstantConditions - expression is hardcoded so we safe from NPE here
                ((UnaryExpression) replacement).getValue().replace(parameter);

                expression.replace(replacement);
            }
        }
    }
}