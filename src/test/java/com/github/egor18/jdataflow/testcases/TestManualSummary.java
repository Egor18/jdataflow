package com.github.egor18.jdataflow.testcases;

public class TestManualSummary
{
    void testMath1(Object o)
    {
        if (20 == Math.max(10, 20)) {} //@ALWAYS_TRUE
        if (10 == Math.min(10, 20)) {} //@ALWAYS_TRUE
        if (10 == Math.abs(10)) {} //@ALWAYS_TRUE
        if (10 == Math.abs(-10)) {} //@ALWAYS_TRUE
        if (20L == Math.max(10L, 20L)) {} //@ALWAYS_TRUE
        if (10L == Math.min(10L, 20L)) {} //@ALWAYS_TRUE
        if (10L == Math.abs(10L)) {} //@ALWAYS_TRUE
        if (10L == Math.abs(-10L)) {} //@ALWAYS_TRUE
    }

    void testEquals1(String a, String b, Integer i, Object o)
    {
        if (a.equals(b)) {} //ok
        if (a.equals(a)) {} //@ALWAYS_TRUE
        if (o.equals(42)) {} //ok
        if (a.equals(42)) {} //ok
        if (i.equals(42)) {} //ok
        if (i.equals(i)) {} //@ALWAYS_TRUE
        if (a.equals(o)) {} //ok
        if (o.equals(o)) {} //@ALWAYS_TRUE
    }
}
