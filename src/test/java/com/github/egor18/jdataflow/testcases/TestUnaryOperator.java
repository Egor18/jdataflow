package com.github.egor18.jdataflow.testcases;

public class TestUnaryOperator
{
    void testNot1()
    {
        boolean a = false;
        if (!a) {} //@ALWAYS_TRUE
    }

    void testCompl1()
    {
        int x = 5;
        if (~x == -6) {} //@ALWAYS_TRUE

        char c = 5;
        if (~c == -6) {} //@ALWAYS_TRUE

        long l = 5;
        if (~l == -6) {} //@ALWAYS_TRUE
    }

    void testIncrement1()
    {
        int c = 1;
        ++c;
        c++;
        if (c == 3) {} //@ALWAYS_TRUE
    }

    void testIncrement2()
    {
        int c = 1;
        c++;
        if (c == 2) {} //@ALWAYS_TRUE
        if (c++ == 2) {} //@ALWAYS_TRUE
        if (c == 3) {} //@ALWAYS_TRUE
    }

    void testIncrement3()
    {
        int d = 1;
        ++d;
        if (d == 2) {} //@ALWAYS_TRUE
        if (++d == 2) {} //@ALWAYS_FALSE
        if (++d == 4) {} //@ALWAYS_TRUE
    }

    void testIncrement4()
    {
        int a = 5;
        int b1 = 5;
        int b2 = 5;
        if (a == b1++) {} //@ALWAYS_TRUE
        if (a == ++b2) {} //@ALWAYS_FALSE
        if (a == (b1 += 4)) {} //@ALWAYS_FALSE
        if (b1 == 10) {} //@ALWAYS_TRUE
    }

    void testDecrement1()
    {
        int c = 1;
        c--;
        if (c == 0) {} //@ALWAYS_TRUE
        if (c-- == 0) {} //@ALWAYS_TRUE
        if (c == -1) {} //@ALWAYS_TRUE
    }

    void testDecrement2()
    {
        int d = 1;
        --d;
        if (d == 0) {} //@ALWAYS_TRUE
        if (--d == 0) {} //@ALWAYS_FALSE
        if (--d == -2) {} //@ALWAYS_TRUE
    }

    void testDecrement3()
    {
        int c = 1;
        c++;
        if (c == 2) {} //@ALWAYS_TRUE
        if (c++ == 3) {} //@ALWAYS_FALSE
        if (c == 3) {} //@ALWAYS_TRUE
        if (--c == 2) {} //@ALWAYS_TRUE
        if (c-- == 1) {} //@ALWAYS_FALSE
        if (c == 1) {} //@ALWAYS_TRUE
    }

    void testIncrementOverflow1()
    {
        byte b = 127;
        if (b++ == 127) {} //@ALWAYS_TRUE
        if (b == -128) {} //@ALWAYS_TRUE
        b = -128;
        if (--b == 127) {} //@ALWAYS_TRUE
        if (b == 127) {} //@ALWAYS_TRUE

        char c = 65535;
        if (c++ == 65535) {} //@ALWAYS_TRUE
        if (c == 0) {} //@ALWAYS_TRUE
        c = 0;
        if (--c == 65535) {} //@ALWAYS_TRUE
        if (c == 65535) {} //@ALWAYS_TRUE

        short s = 32767;
        if (s++ == 32767) {} //@ALWAYS_TRUE
        if (s == -32768) {} //@ALWAYS_TRUE
        s = -32768;
        if (--s == 32767) {} //@ALWAYS_TRUE
        if (s == 32767) {} //@ALWAYS_TRUE

        int i = 2147483647;
        if (i++ == 2147483647) {} //@ALWAYS_TRUE
        if (i == -2147483648) {} //@ALWAYS_TRUE
        i = -2147483648;
        if (--i == 2147483647) {} //@ALWAYS_TRUE
        if (i == 2147483647) {} //@ALWAYS_TRUE
    }

    void testIncrementOverflow2()
    {
        byte b = -128;
        if (--b == -129) {} //@ALWAYS_FALSE
        if (b == 127) {} //@ALWAYS_TRUE
    }

    public Integer field1;
    void testIncrementField1()
    {
        field1++;
        if (field1++ == 0) {} //ok
        if (field1++ == 1) {} //ok
        field1 = 0;
        if (field1++ == 0) {} //@ALWAYS_TRUE
        if (field1++ == 1) {} //@ALWAYS_TRUE
        field1 = 0;
        if (++field1 == 1) {} //@ALWAYS_TRUE
        if (++field1 == 2) {} //@ALWAYS_TRUE
    }

    public int field2;
    void testIncrementField2()
    {
        field2++;
        if (field2++ == 0) {} //ok
        if (field2++ == 1) {} //ok
        field2 = 0;
        if (field2++ == 0) {} //@ALWAYS_TRUE
        if (field2++ == 1) {} //@ALWAYS_TRUE
        field2 = 0;
        if (++field2 == 1) {} //@ALWAYS_TRUE
        if (++field2 == 2) {} //@ALWAYS_TRUE
    }

    public Integer[] fieldArray1;
    void testIncrementFieldArray1(int i)
    {
        fieldArray1[i]++;
        if (fieldArray1[i]++ == 0) {} //ok
        if (fieldArray1[i]++ == 1) {} //ok
        fieldArray1[i] = 0;
        if (fieldArray1[i]++ == 0) {} //@ALWAYS_TRUE
        if (fieldArray1[i]++ == 1) {} //@ALWAYS_TRUE
        fieldArray1[i] = 0;
        if (++fieldArray1[i] == 1) {} //@ALWAYS_TRUE
        if (++fieldArray1[i] == 2) {} //@ALWAYS_TRUE
    }

    public int[] fieldArray2;
    void testIncrementFieldArray2(int i)
    {
        fieldArray2[i]++;
        if (fieldArray2[i]++ == 0) {} //ok
        if (fieldArray2[i]++ == 1) {} //ok
        fieldArray2[i] = 0;
        if (fieldArray2[i]++ == 0) {} //@ALWAYS_TRUE
        if (fieldArray2[i]++ == 1) {} //@ALWAYS_TRUE
        fieldArray2[i] = 0;
        if (++fieldArray2[i] == 1) {} //@ALWAYS_TRUE
        if (++fieldArray2[i] == 2) {} //@ALWAYS_TRUE
    }
}
