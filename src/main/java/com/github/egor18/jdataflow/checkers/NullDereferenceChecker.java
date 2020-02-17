package com.github.egor18.jdataflow.checkers;

import com.github.egor18.jdataflow.misc.ConditionStatus;
import com.github.egor18.jdataflow.scanners.CheckersScanner;
import com.github.egor18.jdataflow.summaries.FunctionSummary;
import com.github.egor18.jdataflow.summaries.interfaces.PredicateFunction;
import com.github.egor18.jdataflow.warning.Warning;
import com.github.egor18.jdataflow.warning.WarningKind;
import com.microsoft.z3.Expr;
import spoon.reflect.code.*;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;

import java.util.List;

import static com.github.egor18.jdataflow.utils.CommonUtils.isEllipsisFunction;

/**
 * This checker warns if null dereference occurs in some expression.
 * For example:
 *    Object x = null;
 *    int z = x.hashCode() // <= null dereference warning
 */
public class NullDereferenceChecker extends AbstractChecker
{
    public NullDereferenceChecker(CheckersScanner scanner)
    {
        super(scanner);
    }

    private void check(CtTargetedExpression<?, ?> targetedExpression)
    {
        CtExpression<?> target = targetedExpression.getTarget();
        if (target == null)
        {
            return;
        }

        Expr targetExpr = (Expr) target.getMetadata("value");
        if (targetExpr == null)
        {
            return;
        }

        ConditionStatus isNull = checkCond(getContext().mkEq(targetExpr, getMemory().nullPointer()));
        if (isNull == ConditionStatus.ALWAYS_TRUE)
        {
            addWarning(new Warning(targetedExpression, WarningKind.NULL_DEREFERENCE));
        }
    }

    private void checkInterprocedural(CtInvocation<?> invocation)
    {
        CtExecutableReference<?> executable = invocation.getExecutable();
        if (executable == null)
        {
            return;
        }

        FunctionSummary functionSummary = getScanner().getFunctionSummary(executable);
        if (functionSummary == null)
        {
            return;
        }

        Expr targetExpr = (Expr) invocation.getTarget().getMetadata("value");
        if (targetExpr == null)
        {
            return;
        }

        List<CtExpression<?>> arguments = invocation.getArguments();
        List<CtTypeReference<?>> formalParameters = executable.getParameters();

        if (arguments.size() != formalParameters.size() || isEllipsisFunction(executable))
        {
            return;
        }

        Expr[] argsExprs = getScanner().getActualArgumentsValues(arguments, formalParameters).toArray(new Expr[0]);

        for (PredicateFunction nullDereferenceCondition : functionSummary.getNullDereferenceConditions())
        {
            if (checkCond(nullDereferenceCondition.apply(targetExpr, argsExprs)) == ConditionStatus.ALWAYS_TRUE)
            {
                addWarning(new Warning(invocation, WarningKind.NULL_DEREFERENCE_INTERPROCEDURAL));
                break;
            }
        }
    }

    @Override
    public void checkInvocation(CtInvocation<?> invocation)
    {
        check(invocation);
        checkInterprocedural(invocation);
    }

    @Override
    public void checkFieldRead(CtFieldRead<?> fieldRead)
    {
        check(fieldRead);
    }

    @Override
    public void checkFieldWrite(CtFieldWrite<?> fieldWrite)
    {
        check(fieldWrite);
    }

    @Override
    public void checkArrayRead(CtArrayRead<?> arrayRead)
    {
        check(arrayRead);
    }

    @Override
    public void checkArrayWrite(CtArrayWrite<?> arrayWrite)
    {
        check(arrayWrite);
    }
}
