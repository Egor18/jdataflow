package com.github.egor18.jdataflow.scanners;

import com.github.egor18.jdataflow.memory.Memory;
import com.microsoft.z3.*;
import spoon.reflect.code.*;
import spoon.reflect.reference.*;
import spoon.reflect.visitor.CtScanner;

import java.util.List;
import java.util.Map;

import static com.github.egor18.jdataflow.utils.CommonUtils.getTargetValue;
import static com.github.egor18.jdataflow.utils.TypeUtils.getActualType;
import static com.github.egor18.jdataflow.utils.TypeUtils.getTypeSort;
import static com.github.egor18.jdataflow.utils.TypeUtils.isImmutable;

/**
 * This scanner resets all the variables (or memory) on their change.
 */
public class ResetOnModificationScanner extends CtScanner
{
    private Context context;
    private Map<CtReference, Expr> variablesMap;
    private Memory memory;

    public ResetOnModificationScanner(Context context, Map<CtReference, Expr> variablesMap, Memory memory)
    {
        this.context = context;
        this.variablesMap = variablesMap;
        this.memory = memory;
    }

    @Override
    public <T> void visitCtVariableWrite(CtVariableWrite<T> variableWrite)
    {
        super.visitCtVariableWrite(variableWrite);
        CtVariableReference<T> reference = variableWrite.getVariable();
        Sort sort = getTypeSort(context, reference.getType());
        variablesMap.put(reference, context.mkFreshConst("", sort));
    }

    @Override
    public <T> void visitCtArrayWrite(CtArrayWrite<T> arrayWrite)
    {
        super.visitCtArrayWrite(arrayWrite);
        CtArrayTypeReference<?> reference = (CtArrayTypeReference) arrayWrite.getTarget().getType();
        IntExpr targetExpr = getTargetValue(context, variablesMap, memory, arrayWrite.getTarget());
        BitVecExpr indexExpr = (BitVecExpr) context.mkFreshConst("", context.mkBitVecSort(32));
        if (targetExpr != null && indexExpr != null)
        {
            Expr valueExpr = context.mkFreshConst("", getTypeSort(context, arrayWrite.getType()));
            memory.writeArray(reference, targetExpr, indexExpr, valueExpr);
        }
    }

    @Override
    public <T> void visitCtFieldWrite(CtFieldWrite<T> fieldWrite)
    {
        super.visitCtFieldWrite(fieldWrite);
        CtFieldReference<T> reference = fieldWrite.getVariable();
        Sort sort = getTypeSort(context, reference.getType());
        IntExpr targetExpr = getTargetValue(context, variablesMap, memory, fieldWrite.getTarget());
        if (targetExpr != null)
        {
            memory.write(reference, targetExpr, context.mkFreshConst("", sort));
        }
    }

    @Override
    public <T> void visitCtConstructorCall(CtConstructorCall<T> ctConstructorCall)
    {
        super.visitCtConstructorCall(ctConstructorCall);
    }

    @Override
    public <T> void visitCtInvocation(CtInvocation<T> invocation)
    {
        super.visitCtInvocation(invocation);

        // Reset arguments
        List<CtExpression<?>> arguments = invocation.getArguments();
        for (CtExpression<?> argument : arguments)
        {
            CtTypeReference<?> argumentType = getActualType(argument);
            if (!argumentType.isPrimitive() && !isImmutable(argumentType))
            {
                // Getting the value of a non-primitive argument
                IntExpr argumentExpr = getTargetValue(context, variablesMap, memory, argument);
                if (argumentExpr != null)
                {
                    memory.resetObject(argumentType, argumentExpr);
                }
            }
        }

        // Reset target
        CtExpression<?> target = invocation.getTarget();
        if (target != null)
        {
            if (!(target instanceof CtTypeAccess))
            {
                CtTypeReference<?> targetType = getActualType(target);
                if (!isImmutable(targetType))
                {
                    IntExpr targetExpr = getTargetValue(context, variablesMap, memory, target);
                    if (targetExpr != null)
                    {
                        memory.resetObject(targetType, targetExpr);
                    }
                }
            }
        }
    }
}
