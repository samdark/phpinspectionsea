package com.kalessil.phpStorm.phpInspectionsEA.inspectors.codeSmell;


import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.elements.*;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpElementVisitor;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpInspection;
import org.jetbrains.annotations.NotNull;

public class UnnecessarySemicolonInspector extends BasePhpInspection {
    private static final String message = "Unnecessary semicolon";

    @NotNull
    public String getShortName() {
        return "UnnecessarySemicolonInspection";
    }

    @Override
    @NotNull
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new BasePhpElementVisitor() {
            public void visitPhpStatement(Statement statement) {
                if (0 == statement.getChildren().length) {
                    final PsiElement parent = statement.getParent();
                    if (null != parent) {
                        final IElementType declareCandidate = statement.getParent().getFirstChild().getNode().getElementType();
                        if (
                            PhpTokenTypes.kwDECLARE == declareCandidate ||
                            parent instanceof DoWhile ||
                            parent instanceof While ||
                            parent instanceof For ||
                            parent instanceof ForeachStatement ||
                            parent instanceof If ||
                            parent instanceof ElseIf ||
                            parent instanceof Else
                        ) {
                            return;
                        }
                    }

                    holder.registerProblem(statement, message, ProblemHighlightType.WEAK_WARNING, new TheLocalFix());
                }
            }
        };
    }

    private static class TheLocalFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() {
            return "Drop it";
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return getName();
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            final PsiElement expression = descriptor.getPsiElement();
            if (expression instanceof Statement) {
                /* drop preceding space */
                if (expression.getPrevSibling() instanceof PsiWhiteSpace) {
                    expression.getPrevSibling().delete();
                }

                expression.delete();
            }
        }
    }
}

