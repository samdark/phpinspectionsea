package com.kalessil.phpStorm.phpInspectionsEA.inspectors.apiUsage;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.ide.PsiActionSupportFactory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.search.PsiSearchHelperImpl;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.PsiSearchRequest;
import com.intellij.psi.search.PsiSearchScopeUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.*;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpElementVisitor;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpInspection;
import com.kalessil.phpStorm.phpInspectionsEA.utils.ExpressionSemanticUtil;
import com.kalessil.phpStorm.phpInspectionsEA.utils.TypeFromPsiResolvingUtil;
import com.kalessil.phpStorm.phpInspectionsEA.utils.Types;
import com.kalessil.phpStorm.phpInspectionsEA.utils.TypesSemanticsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

public class IsEmptyFunctionUsageInspector extends BasePhpInspection {
    private static final String strProblemDescriptionDoNotUse = "'empty(...)' counts too much values as empty, consider refactoring with type sensitive checks";
    private static final String strProblemDescriptionUseCount = "'count($...) === 0' construction shall be used instead";
    private static final String strProblemDescriptionUseNullComparison = "Probably it should be 'null === $...' construction used";
    
    @NotNull
    public String getShortName() {
        return "IsEmptyFunctionUsageInspection";
    }

    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new BasePhpElementVisitor() {
            public void visitPhpEmpty(PhpEmpty emptyExpression) {

                /* find .php_cs in projects root */
                Project currentProject = holder.getProject();
                VirtualFile csFixerVirtualFile = currentProject.getBaseDir().findChild(".php_cs");
                if (null != csFixerVirtualFile) {
                    /* ensure file is recognized as PHP code */
                    if (PhpFileType.INSTANCE != csFixerVirtualFile.getFileType()) {
                        FileTypeManager.getInstance().associateExtension(PhpFileType.INSTANCE, "php_cs");
                    }

                    /* Get PSI file from Virtual one */
                    PsiFile csFixerFile = PsiManager.getInstance(currentProject).findFile(csFixerVirtualFile);
                    if (null != csFixerFile) {
                        PsiTreeUtil.findChildrenOfType(csFixerFile, MethodReference.class);
                        /* iterate fond references */
                    }

                    // listen for changes
                    // PsiManager.getInstance(project).addPsiTreeChangeListener()
                }


                PhpExpression[] arrValues = emptyExpression.getVariables();
                if (arrValues.length == 1) {
                    PsiElement objParameterToInspect = ExpressionSemanticUtil.getExpressionTroughParenthesis(arrValues[0]);
                    if (objParameterToInspect instanceof ArrayAccessExpression) {
                        /** currently php docs lacks of array structure notations, skip it */
                        return;
                    }


                    /** extract types */
                    PhpIndex objIndex = PhpIndex.getInstance(holder.getProject());
                    Function objScope = ExpressionSemanticUtil.getScope(emptyExpression);
                    HashSet<String> objResolvedTypes = new HashSet<String>();
                    TypeFromPsiResolvingUtil.resolveExpressionType(objParameterToInspect, objScope, objIndex, objResolvedTypes);

                    /** Case 1: empty(array) - hidden logic - empty array */
                    if (this.isArrayType(objResolvedTypes)) {
                        objResolvedTypes.clear();
                        holder.registerProblem(emptyExpression, strProblemDescriptionUseCount, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                        return;
                    }

                    /** case 2: nullable classes, int, float, resource */
                    if (this.isNullableCoreType(objResolvedTypes) || TypesSemanticsUtil.isNullableObjectInterface(objResolvedTypes)) {
                        objResolvedTypes.clear();
                        holder.registerProblem(emptyExpression, strProblemDescriptionUseNullComparison, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                        return;
                    }

                    objResolvedTypes.clear();
                }

                holder.registerProblem(emptyExpression, strProblemDescriptionDoNotUse, ProblemHighlightType.WEAK_WARNING);
            }


            /** check if only array type possible */
            private boolean isArrayType(HashSet<String> resolvedTypesSet) {
                return resolvedTypesSet.size() == 1 && resolvedTypesSet.contains(Types.strArray);
            }

            /** check if nullable int, float, resource */
            private boolean isNullableCoreType(HashSet<String> resolvedTypesSet) {
                //noinspection SimplifiableIfStatement
                if (resolvedTypesSet.size() != 2 || !resolvedTypesSet.contains(Types.strNull)) {
                    return false;
                }

                return  resolvedTypesSet.contains(Types.strInteger) ||
                        resolvedTypesSet.contains(Types.strFloat) ||
                        resolvedTypesSet.contains(Types.strResource);
            }
        };
    }
}
