package com.github.egor18.jdataflow.testcases;

import java.util.ArrayList;

public class TestExecutableReference
{
    native void function();
    static native void staticFunction();
    native Object g(Runnable a, Runnable b, Runnable c);

    public Runnable r1 = this::function;
    public Runnable r2 = TestExecutableReference::staticFunction;

    void testExecutableReference1()
    {
        Runnable func = this::function;
        if (this == func) {}
        if (func == null) {} //@ALWAYS_FALSE
        if (r1 == null) {} //ok
        if (r2 == null) {} //ok
    }

    void testExecutableReference2(TestExecutableReference other)
    {
        Runnable func = other::function;
        if (func == null) {} //@ALWAYS_FALSE
    }

    void testExecutableReference3()
    {
        g(TestExecutableReference::new, this::function, ArrayList<Integer>::new);
    }
}
