package com.github.egor18.jdataflow.summaries;

import com.microsoft.z3.Expr;
import spoon.reflect.reference.CtTypeReference;

import java.util.Arrays;

public class FunctionArgument
{
    public Expr expr;

    public CtTypeReference<?> type;

    public FunctionArgument(Expr expr, CtTypeReference<?> type)
    {
        this.expr = expr;
        this.type = type;
    }

    public static FunctionArgument[] getFunctionArguments(Expr[] argsExprs, CtTypeReference<?>[] argsTypes)
    {
        FunctionArgument[] args = new FunctionArgument[argsExprs.length];
        Arrays.setAll(args, i -> new FunctionArgument(argsExprs[i], argsTypes[i]));
        return args;
    }
}
