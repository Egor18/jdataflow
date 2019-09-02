package com.github.egor18.jdataflow.summaries.interfaces;

import com.microsoft.z3.Expr;

public interface ReturnFunction
{
    Expr apply(Expr target, Expr[] args);
}
