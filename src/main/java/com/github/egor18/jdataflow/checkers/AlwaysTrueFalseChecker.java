package com.github.egor18.jdataflow.checkers;

import com.github.egor18.jdataflow.misc.ConditionStatus;
import com.github.egor18.jdataflow.scanners.CheckersScanner;
import com.github.egor18.jdataflow.utils.TypeUtils;
import com.github.egor18.jdataflow.warning.Warning;
import com.github.egor18.jdataflow.warning.WarningKind;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import spoon.reflect.code.*;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtTypeReference;

import static com.github.egor18.jdataflow.utils.TypeUtils.getActualType;
import static com.github.egor18.jdataflow.utils.TypeUtils.isCalculable;

/**
 * This checker warns if some expression is always true/false.
 * For example:
 *    int a = 5;
 *    if (a == 5) {} // <= always true warning
 */
public class AlwaysTrueFalseChecker extends AbstractChecker
{
    public AlwaysTrueFalseChecker(CheckersScanner scanner)
    {
        super(scanner);
    }

    private boolean isLoopCondition(CtElement parent, CtExpression<?> expression)
    {
        return (parent instanceof CtWhile && ((CtWhile) parent).getLoopingExpression() == expression)
                || (parent instanceof CtDo && ((CtDo) parent).getLoopingExpression() == expression)
                || (parent instanceof CtFor && ((CtFor) parent).getExpression() == expression);
    }

    private void check(CtExpression<?> expression)
    {
        CtTypeReference<?> expressionType = getActualType(expression);
        if (!isCalculable(expressionType))
        {
            return;
        }

        // There is no need to warn about while (true) {...}
        if (expression instanceof CtLiteral
            && ((CtLiteral<?>) expression).getValue().equals(true)
            && isLoopCondition(expression.getParent(), expression))
        {
            return;
        }

        Expr conditionExpr = (Expr) expression.getMetadata("value");

        // Unboxing conversion
        if (!expressionType.isPrimitive())
        {
            conditionExpr = getMemory().read(expressionType.unbox(), (IntExpr) conditionExpr);
        }

        ConditionStatus conditionStatus = checkCond((BoolExpr) conditionExpr);

        if (conditionStatus == ConditionStatus.ALWAYS_TRUE)
        {
            // On the first iteration we should not warn about always true expressions inside loop entry condition
            if (isInsideLoopEntryCondition())
            {
                return;
            }
            addWarning(new Warning(expression, WarningKind.ALWAYS_TRUE));
        }
        else if (conditionStatus == ConditionStatus.ALWAYS_FALSE)
        {
            // On the first iteration we should warn only about always false loop entry condition
            if (isInsideLoopEntryCondition() && !isLoopCondition(expression.getParent(CtLoop.class), expression))
            {
                return;
            }
            addWarning(new Warning(expression, WarningKind.ALWAYS_FALSE));
        }
    }

    private void checkExpression(CtExpression<?> expression)
    {
        if (expression != null && !(expression instanceof CtLiteral) && TypeUtils.isBoolean(expression.getType()))
        {
            check(expression);
        }
    }

    @Override
    public void checkCondition(CtExpression<?> condition)
    {
        check(condition);
    }

    @Override
    public void checkBinaryOperatorLeft(BinaryOperatorKind kind, CtExpression<?> left)
    {
        if (kind == BinaryOperatorKind.AND || kind == BinaryOperatorKind.OR)
        {
            check(left);
        }
    }

    @Override
    public void checkBinaryOperatorRight(BinaryOperatorKind kind, CtExpression<?> right)
    {
        if (kind == BinaryOperatorKind.AND || kind == BinaryOperatorKind.OR)
        {
            check(right);
        }
    }

    @Override
    public void checkConditionalThenExpression(CtExpression<?> thenExpression)
    {
        checkExpression(thenExpression);
    }

    @Override
    public void checkConditionalElseExpression(CtExpression<?> elseExpression)
    {
        checkExpression(elseExpression);
    }

    @Override
    public void checkReturnedExpression(CtExpression<?> returnedExpression)
    {
        checkExpression(returnedExpression);
    }

    @Override
    public void checkAssignmentRight(CtExpression<?> right)
    {
        // There is no need to warn about a = b = c = true or a = constant
        if (!(right instanceof CtAssignment) && !(right instanceof CtVariableRead))
        {
            checkExpression(right);
        }
    }
}
