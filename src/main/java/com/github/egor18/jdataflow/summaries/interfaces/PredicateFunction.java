package com.github.egor18.jdataflow.summaries.interfaces;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Expr;

public interface PredicateFunction
{
    BoolExpr apply(Expr target, Expr[] args);
}
