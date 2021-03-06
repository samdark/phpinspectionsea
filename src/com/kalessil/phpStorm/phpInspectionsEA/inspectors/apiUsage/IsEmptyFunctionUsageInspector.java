package com.kalessil.phpStorm.phpInspectionsEA.inspectors.apiUsage;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.ArrayAccessExpression;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.PhpEmpty;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpElementVisitor;
import com.kalessil.phpStorm.phpInspectionsEA.openApi.BasePhpInspection;
import com.kalessil.phpStorm.phpInspectionsEA.utils.ExpressionSemanticUtil;
import com.kalessil.phpStorm.phpInspectionsEA.utils.TypeFromPsiResolvingUtil;
import com.kalessil.phpStorm.phpInspectionsEA.utils.Types;
import com.kalessil.phpStorm.phpInspectionsEA.utils.TypesSemanticsUtil;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.HashSet;

public class IsEmptyFunctionUsageInspector extends BasePhpInspection {
    // configuration flags automatically saved by IDE
    @SuppressWarnings("WeakerAccess")
    public boolean REPORT_EMPTY_USAGE = true;
    @SuppressWarnings("WeakerAccess")
    public boolean SUGGEST_TO_USE_COUNT_CHECK = true;
    @SuppressWarnings("WeakerAccess")
    public boolean SUGGEST_TO_USE_NULL_COMPARISON = true;

    // static messages for triggered messages
    private static final String strProblemDescriptionDoNotUse = "'empty(...)' counts too many values as empty, consider refactoring with type sensitive checks";
    private static final String strProblemDescriptionUseCount = "'0 === count($...)' construction shall be used instead";
    private static final String strProblemDescriptionUseNullComparison = "Probably it should be 'null === $...' construction used";
    
    @NotNull
    public String getShortName() {
        return "IsEmptyFunctionUsageInspection";
    }

    @Override
    @NotNull
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new BasePhpElementVisitor() {
            public void visitPhpEmpty(PhpEmpty emptyExpression) {
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

                        if (SUGGEST_TO_USE_COUNT_CHECK) {
                            holder.registerProblem(emptyExpression, strProblemDescriptionUseCount, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                        }

                        return;
                    }

                    /** case 2: nullable classes, int, float, resource */
                    if (this.isNullableCoreType(objResolvedTypes) || TypesSemanticsUtil.isNullableObjectInterface(objResolvedTypes)) {
                        objResolvedTypes.clear();

                        if (SUGGEST_TO_USE_NULL_COMPARISON) {
                            holder.registerProblem(emptyExpression, strProblemDescriptionUseNullComparison, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                        }

                        return;
                    }

                    objResolvedTypes.clear();
                }

                if (REPORT_EMPTY_USAGE) {
                    holder.registerProblem(emptyExpression, strProblemDescriptionDoNotUse, ProblemHighlightType.WEAK_WARNING);
                }
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

    public JComponent createOptionsPanel() {
        return (new IsEmptyFunctionUsageInspector.OptionsPanel()).getComponent();
    }

    public class OptionsPanel {
        final private JPanel optionsPanel;

        final private JCheckBox reportEmptyUsage;
        final private JCheckBox suggestToUseCountComparison;
        final private JCheckBox suggestToUseNullComparison;

        public OptionsPanel() {
            optionsPanel = new JPanel();
            optionsPanel.setLayout(new MigLayout());

            reportEmptyUsage = new JCheckBox("Report empty() usage", REPORT_EMPTY_USAGE);
            reportEmptyUsage.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    REPORT_EMPTY_USAGE = reportEmptyUsage.isSelected();
                }
            });
            optionsPanel.add(reportEmptyUsage, "wrap");

            suggestToUseCountComparison = new JCheckBox("Suggest to use count()-comparison", SUGGEST_TO_USE_COUNT_CHECK);
            suggestToUseCountComparison.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    SUGGEST_TO_USE_COUNT_CHECK = suggestToUseCountComparison.isSelected();
                }
            });
            optionsPanel.add(suggestToUseCountComparison, "wrap");

            suggestToUseNullComparison = new JCheckBox("Suggest to use null-comparison", SUGGEST_TO_USE_NULL_COMPARISON);
            suggestToUseNullComparison.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    SUGGEST_TO_USE_NULL_COMPARISON = suggestToUseNullComparison.isSelected();
                }
            });
            optionsPanel.add(suggestToUseNullComparison, "wrap");
        }

        public JPanel getComponent() {
            return optionsPanel;
        }
    }
}
