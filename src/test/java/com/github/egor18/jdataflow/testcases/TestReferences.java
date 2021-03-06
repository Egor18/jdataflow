package com.github.egor18.jdataflow.testcases;

import java.util.Map;

public class TestReferences
{
    void testReferenceCompare1(Object o1, Object o2)
    {
        if (o1 == o2) {} //ok
        o1 = new Object();
        if (o1 == o2) {} //ok (o2 is unknown)
    }

    void testReferencePotentialUnknown(Object o1, Object o2, boolean cond, Object unknown)
    {
        o1 = new Object();
        o2 = new Object();
        if (cond)
        {
            o2 = unknown;
        }
        if (o1 == o2) {} //ok (o2 is possibly unknown)
    }

    void testReferenceCompare2(Object o1, Object o2)
    {
        o1 = new Object();
        o2 = new Object();
        if (o1 == o2) {} //@ALWAYS_FALSE
    }

    void testReferenceCompare3()
    {
        Object o1 = new Object();
        Object o2 = new Object();
        if (o1 == o2) {} //@ALWAYS_FALSE
    }

    void testReferenceCompare4(Object o1)
    {
        if (o1 == null) {} //ok
        o1 = new Object();
        if (o1 == null) {} //@ALWAYS_FALSE
    }

    void testReferenceCompare5()
    {
        if (null == null) {} //@ALWAYS_TRUE
        Object o1 = null;
        if (o1 == null) {} //@ALWAYS_TRUE
    }

    void testReferenceIf1(boolean cond)
    {
        Integer a = new Integer(42);
        if (a == null) {} //@ALWAYS_FALSE
        if (cond)
        {
            if (a == null) {} //@ALWAYS_FALSE
            a = null;
        }
        if (a == null) {} //ok
    }

    void testReferences1(Object a, Object b, Object c, Object d)
    {
        a = new Object();
        b = a;
        c = b;
        d = c;
        if (d == null) {} //@ALWAYS_FALSE
        if (d == a) {} //@ALWAYS_TRUE
    }

    void testReferenceBinOp1()
    {
        Integer a = new Integer(42);
        Integer b = new Integer(32);
        if (a >= b) {} //@ALWAYS_TRUE
        if (a < b) {} //@ALWAYS_FALSE
        if (a == b) {} //@ALWAYS_FALSE

        if (a == (b + 10)) {} //@ALWAYS_TRUE
        if (a / 10 > 10) {} //@ALWAYS_FALSE
    }

    void testReferenceUnary1()
    {
        Integer a = new Integer(42);
        if (-a > 0) {} //@ALWAYS_FALSE

        Integer b = a;
        Integer c = b;
        if (-c < 0) {} //@ALWAYS_TRUE
    }

    void testThis1()
    {
        if (this == null){} //@ALWAYS_FALSE
        TestReferences a = this;
        if (a == null) {} //@ALWAYS_FALSE
        if (a == this) {} //@ALWAYS_TRUE
        if (this == this) {} //@ALWAYS_TRUE
    }

    private final TestReferences self = new TestReferences();
    void testThis2()
    {
        if (self == null) {} //@ALWAYS_FALSE
        if (this == self) {} //ok
    }

    void testStringLiteral1()
    {
        if ("abcdef" == null) {} //@ALWAYS_FALSE
    }

    void testStrings1(String something)
    {
        String a = "one";
        String b = "two";
        String c = "three";
        if ((a + b + c).equals(something)) {}
    }

    class TestThisAccess
    {
        int x;
        void f1()
        {
            this.x = 142;
            if (x == 142) {} //@ALWAYS_TRUE
        }
    }

    class TestConstructor1
    {
        public Integer x;

        TestConstructor1(Integer x)
        {
            this.x = x;
            if (this.x == x) {} //@ALWAYS_TRUE
        }

        void f(Integer x)
        {
            this.x = x;
            if (this.x == x) {} //@ALWAYS_TRUE
        }
    }

    class Base {}
    class Derived extends Base {}
    void testBaseDerivedReferences(Base a, Derived b)
    {
        if (a == b) {} //ok
        a = b;
        if (a == b) {} //@ALWAYS_TRUE
    }

    class TestSuper
    {
        class Base
        {
            public int x;
            public int y;
            public void f() {}
        }

        class Derived extends Base
        {
            public int y;

            @Override
            public void f()
            {
                super.f();
                super.x = 42;
                if (super.x == 42) {} //@ALWAYS_TRUE
                if (super.x == this.x) {} //@ALWAYS_TRUE
                if (super.y == this.y) {} //ok
            }
        }
    }

    void testDifferentArrayReferences1(Map<String, Class[]> m1)
    {
        Class<?>[] clazz1 = new Class[]{ String.class };
        m1.put("1", clazz1);
    }

    void testDifferentArrayReferences2(Map<String, Class<?>[]> m1, Map<String, Class[]> m2)
    {
        Class<?>[] clazz1 = new Class[]{ String.class };
        Class<?>[] clazz2 = new Class<?>[]{ String.class };
        Class[] clazz3 = new Class[]{ String.class };
        Class[] clazz4 = new Class<?>[]{ String.class };
        m1.put("1", clazz1);
        m1.put("2", clazz2);
        m1.put("3", clazz3);
        m1.put("4", clazz4);
        m2.put("1", clazz1);
        m2.put("2", clazz2);
        m2.put("3", clazz3);
        m2.put("4", clazz4);
    }

    class TestSegfault
    {
        private long value;

        public void testSegfault(TestSegfault v)
        {
            long x = this.value;
            long y = v.value;
            long result = x + y;

            if (((x ^ result) & (y ^ result)) < 0) //ok (no segfault)
            {
                throw new RuntimeException("error");
            }
        }
    }
}
