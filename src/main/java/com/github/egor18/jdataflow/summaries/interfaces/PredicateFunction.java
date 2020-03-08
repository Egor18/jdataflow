package com.github.egor18.jdataflow.summaries.interfaces;

import com.github.egor18.jdataflow.summaries.FunctionArgument;
import com.github.egor18.jdataflow.summaries.FunctionTarget;
import com.microsoft.z3.BoolExpr;

public interface PredicateFunction
{
    BoolExpr apply(FunctionTarget target, FunctionArgument[] args);
}
