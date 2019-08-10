package com.github.egor18.jdataflow.utils;

import com.github.egor18.jdataflow.memory.Memory;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.IntExpr;
import spoon.reflect.code.*;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtReference;
import spoon.reflect.reference.CtTypeReference;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

import static com.github.egor18.jdataflow.utils.TypeUtils.getActualType;

public final class CommonUtils
{
    private CommonUtils() {}

    /**
     * Gets target expression value by going from left to right.
     * For example, 'a.b' is calculated as follows: memory[T2.b][memory[T1.a][a]]
     */
    public static IntExpr getTargetValue(Context context, Map<CtReference, Expr> variablesMap, Memory memory, CtExpression<?> target)
    {
        Deque<CtExpression> targets = new ArrayDeque<>();
        while (target instanceof CtTargetedExpression)
        {
            targets.addFirst(target);
            target = ((CtTargetedExpression) target).getTarget();
        }

        if (target != null)
        {
            targets.addFirst(target);
        }

        // Traverse all targets left to right
        IntExpr targetValue = null;
        for (CtExpression t : targets)
        {
            if (t instanceof CtFieldRead)
            {
                targetValue = (IntExpr) memory.read(((CtFieldRead) t).getVariable(), targetValue);
            }
            else if (t instanceof CtArrayRead)
            {
                CtArrayRead arrayRead = (CtArrayRead) t;
                CtExpression index = arrayRead.getIndexExpression();
                Expr arrayIndex = (Expr) index.getMetadata("value");
                if (arrayIndex == null)
                {
                    arrayIndex = context.mkFreshConst("", context.mkBitVecSort(32));
                }
                targetValue = (IntExpr) memory.readArray((CtArrayTypeReference) arrayRead.getTarget().getType(), targetValue, arrayIndex);
            }
            else if (t instanceof CtThisAccess || t instanceof CtSuperAccess)
            {
                targetValue = context.mkInt(Memory.thisPointer());
            }
            else if (t instanceof CtVariableRead)
            {
                targetValue = (IntExpr) variablesMap.get(((CtVariableRead) t).getVariable());
            }
            else if (t instanceof CtTypeAccess)
            {
                CtTypeReference accessedType = ((CtTypeAccess) t).getAccessedType();
                targetValue = (IntExpr) variablesMap.get(accessedType);
                if (targetValue == null)
                {
                    targetValue = (IntExpr) context.mkFreshConst("", context.getIntSort());
                    variablesMap.put(accessedType, targetValue);
                }
            }
            else
            {
                // Impure functions and other unknown stuff
                targetValue = (IntExpr) context.mkFreshConst("", context.getIntSort());
            }
        }

        return targetValue;
    }
}
