package com.github.egor18.jdataflow.summaries;

import com.github.egor18.jdataflow.summaries.interfaces.EffectFunction;
import com.github.egor18.jdataflow.summaries.interfaces.ReturnFunction;
import com.microsoft.z3.FuncDecl;

import java.util.ArrayList;
import java.util.List;

public class FunctionSummary
{
    // Whether this summary is hardcoded manually or deduced
    private boolean isManual;

    // Whether this function is pure i.e.:
    // Its return value is the same for the same arguments (no variation with non-local variables, mutable reference arguments or I/O).
    // Its evaluation has no side effects (no mutation of non-local variables, mutable reference arguments or I/O).
    // https://en.wikipedia.org/wiki/Pure_function
    private boolean isPure;

    // Calculated return value of this function
    private ReturnFunction returnFunction;

    // Symbolic return value of this function
    private FuncDecl symbolicReturnFunction;

    // Known effects produced by this function invocation
    private List<EffectFunction> effectFunctions = new ArrayList<>();

    // Indices of read-only (non-modifiable) arguments
    private List<Integer> readOnlyArguments = new ArrayList<>();

    // Whether target is read-only (non-modifiable)
    private boolean isReadOnlyTarget;

    public ReturnFunction getReturnFunc()
    {
        return returnFunction;
    }

    public FunctionSummary pure()
    {
        this.isPure = true;
        return this;
    }

    public boolean isPure()
    {
        return isPure;
    }

    public void setPure(boolean isPure)
    {
        this.isPure = isPure;
    }

    public boolean isManual()
    {
        return isManual;
    }

    public void setManual(boolean isManual)
    {
        this.isManual = isManual;
    }

    public FunctionSummary setReturn(ReturnFunction returnFunction)
    {
        if (symbolicReturnFunction != null)
        {
            throw new RuntimeException("Summary should have only one return function");
        }
        this.returnFunction = returnFunction;
        return this;
    }

    public FuncDecl getSymbolicReturn()
    {
        return symbolicReturnFunction;
    }

    public FunctionSummary setSymbolicReturn(FuncDecl symbolicReturnFunction)
    {
        if (returnFunction != null)
        {
            throw new RuntimeException("Summary should have only one return function");
        }
        this.symbolicReturnFunction = symbolicReturnFunction;
        return this;
    }

    public FunctionSummary addEffect(EffectFunction effectFunction)
    {
        this.effectFunctions.add(effectFunction);
        return this;
    }

    public List<EffectFunction> getEffects()
    {
        return effectFunctions;
    }

    public FunctionSummary readOnlyArgument(Integer argumentIndex)
    {
        readOnlyArguments.add(argumentIndex);
        return this;
    }

    public List<Integer> getReadOnlyArguments()
    {
        return readOnlyArguments;
    }

    public void setReadOnlyArguments(List<Integer> readOnlyArguments)
    {
        this.readOnlyArguments = readOnlyArguments;
    }

    public boolean isReadOnlyTarget()
    {
        return isReadOnlyTarget;
    }

    public void setReadOnlyTarget(boolean isReadOnlyTarget)
    {
        this.isReadOnlyTarget = isReadOnlyTarget;
    }

    public FunctionSummary readOnlyTarget()
    {
        isReadOnlyTarget = true;
        return this;
    }
}
