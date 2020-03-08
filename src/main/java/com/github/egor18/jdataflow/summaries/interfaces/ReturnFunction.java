package com.github.egor18.jdataflow.summaries.interfaces;

import com.github.egor18.jdataflow.summaries.FunctionArgument;
import com.github.egor18.jdataflow.summaries.FunctionTarget;
import com.microsoft.z3.Expr;

public interface ReturnFunction
{
    Expr apply(FunctionTarget target, FunctionArgument[] args);
}
