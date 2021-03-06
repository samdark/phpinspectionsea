package com.kalessil.phpStorm.phpInspectionsEA.inspectors.languageConstructions;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiWhiteSpace;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpElementVisitor;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpInspection;
import org.jetbrains.annotations.NotNull;

public class SenselessCommaInArrayDefinitionInspector extends BasePhpInspection {
    private static final String strProblemDescription = "Can be safely dropped. The comma will be ignored by PHP";

    @NotNull
    public String getShortName() {
        return "SenselessCommaInArrayDefinitionInspection";
    }

    @Override
    @NotNull
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new BasePhpElementVisitor() {
            public void visitPhpArrayCreationExpression(ArrayCreationExpression expression) {
                PsiElement objExpressionToTest = expression.getLastChild().getPrevSibling();
                if (objExpressionToTest instanceof PsiWhiteSpace) {
                    objExpressionToTest = objExpressionToTest.getPrevSibling();
                }
                if (null == objExpressionToTest) {
                    return;
                }

                if (objExpressionToTest.getNode().getElementType() == PhpTokenTypes.opCOMMA) {
                    holder.registerProblem(objExpressionToTest, strProblemDescription, ProblemHighlightType.WEAK_WARNING);
                }
            }
        };
    }
}
