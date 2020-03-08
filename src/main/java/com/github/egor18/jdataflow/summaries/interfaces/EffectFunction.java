package com.github.egor18.jdataflow.summaries.interfaces;

import com.github.egor18.jdataflow.summaries.FunctionArgument;
import com.github.egor18.jdataflow.summaries.FunctionTarget;

public interface EffectFunction
{
    void apply(FunctionTarget target, FunctionArgument[] args);
}
