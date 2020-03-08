package com.github.egor18.jdataflow.summaries;

import com.microsoft.z3.Expr;
import spoon.reflect.reference.CtTypeReference;

public class FunctionTarget
{
    public Expr expr;

    public CtTypeReference<?> type;

    public FunctionTarget(Expr expr, CtTypeReference<?> type)
    {
        this.expr = expr;
        this.type = type;
    }

    public static FunctionTarget getFunctionTarget(Expr targetExpr, CtTypeReference<?> targetType)
    {
        return new FunctionTarget(targetExpr, targetType);
    }
}
