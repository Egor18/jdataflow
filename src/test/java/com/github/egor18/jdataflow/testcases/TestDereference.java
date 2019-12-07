package com.github.egor18.jdataflow.testcases;

public class TestDereference
{
    private class C1 { int x; }

    void testDereference1(String s)
    {
        int x = s.length();
        if (s == null) {} //@ALWAYS_FALSE
    }

    void testDereference2(C1 a)
    {
        a.x = 5;
        C1 b = a;
        if (a == null) {} //@ALWAYS_FALSE
        if (b == null) {} //@ALWAYS_FALSE
    }

    void testDereference3(C1 a)
    {
        int z = a.x;
        if (a == null) {} //@ALWAYS_FALSE
    }

    void testDereference4(int[] arr)
    {
        arr[0] = 4;
        if (arr == null) {} //@ALWAYS_FALSE
    }

    void testDereference5(int[] arr)
    {
        int a = arr[0];
        if (arr == null) {} //@ALWAYS_FALSE
    }

    String testDereference6(String s)
    {
        return s == null ? s : s.replace('\r', ' ').replace('\n', ' '); //ok
    }

    void testDereference7(Object x, boolean cond)
    {
        if (cond)
        {
            int z = x.hashCode();
        }

        if (cond)
        {
            if (x == null) {} //@ALWAYS_FALSE
        }
    }

    void testDereference8(Object x)
    {
        if (x == null || x.hashCode() == 10) {} //ok
        if (x == null) {} //ok
    }
}
