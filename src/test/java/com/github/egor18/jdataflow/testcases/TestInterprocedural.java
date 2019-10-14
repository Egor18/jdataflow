package com.github.egor18.jdataflow.testcases;

public class TestInterprocedural
{
    void testManualSummary1(Object o)
    {
        if (20 == Math.max(10, 20)) {} //@ALWAYS_TRUE
        if (10 == Math.min(10, 20)) {} //@ALWAYS_TRUE
        if (10 == Math.abs(10)) {} //@ALWAYS_TRUE
        if (10 == Math.abs(-10)) {} //@ALWAYS_TRUE
        if (o.equals(o)) {} //@ALWAYS_TRUE
    }

    private int f1(int x)
    {
        return x + 1;
    }

    private int f2(int x)
    {
        return f1(x);
    }

    private int f3(int x)
    {
        return f2(x);
    }

    class MyClass
    {
        public int x;
    }

    private int getX(MyClass o)
    {
        return o.x;
    }

    void testPure1(int x)
    {
        if (f1(42) == f1(42)) {} //@ALWAYS_TRUE
        if (f1(x) == f1(x)) {} //@ALWAYS_TRUE
        if (f3(42) == f3(42)) {} //@ALWAYS_TRUE
        if (f3(x) == f3(x)) {} //@ALWAYS_TRUE
    }

    void testArgumentsCast1()
    {
        byte b1 = 10;
        byte b2 = 20;
        if (20 == Math.max(b1, b2)) {} //@ALWAYS_TRUE
        if (10 == Math.min(b1, b2)) {} //@ALWAYS_TRUE
        if (10 == Math.abs(b1)) {} //@ALWAYS_TRUE
        if (10 == Math.abs(-b1)) {} //@ALWAYS_TRUE
        if (f1(new Integer(42)) == f1(42)) {} //@ALWAYS_TRUE
        if (f1(b1) == f1(b1)) {} //@ALWAYS_TRUE
        if (f1((int) b1) == f1((int) b1)) {} //@ALWAYS_TRUE
        if (f1((int) b1) == f1((char) b1)) {} //@ALWAYS_TRUE
    }

    void testImpure1()
    {
        MyClass o = new MyClass();
        o.x = 10;
        int a = getX(o);
        o.x = 20;
        int b = getX(o);
        if (a == b) {} //ok
    }

    private void throwException()
    {
        throw new RuntimeException("Exception");
    }

    private void throwException(boolean cond)
    {
        if (cond)
        {
            throw new RuntimeException("Exception");
        }
    }

    void throwException2(boolean cond)
    {
        if (cond)
        {
            return;
        }
        throw new RuntimeException("Exception");
    }

    void testThrow1(boolean x)
    {
        if (x)
        {
            throwException();
        }
        if (x) {} //@ALWAYS_FALSE
    }

    void testThrow2(boolean cond, boolean x)
    {
        if (x)
        {
            throwException(cond);
        }
        if (x) {} //ok
    }

    void testThrow3(boolean cond)
    {
        throwException();
        if (cond) {} //TODO: warn about unreachable code (new checker)
    }

    void testThrow4(boolean cond, boolean x)
    {
        if (x)
        {
            throwException(cond);
        }
        if (x) {} //ok
    }

    private int r1(int depth)
    {
        if (depth > 0)
        {
            return r1(depth - 1);
        }
        return 0;
    }

    private int r2()
    {
        return r3();
    }

    private int r3()
    {
        return r4();
    }

    private int r4()
    {
        return r2();
    }

    void testRecursive1()
    {
        if (r1(10) == r1(10)) {}
    }

    void testRecursive2()
    {
        if (r2() == r2()) {}
        if (r3() == r3()) {}
        if (r4() == r4()) {}
    }

    private int e1(int... args)
    {
        return args.length;
    }

    void testEllipsis1()
    {
        if (e1(10) == e1()) {}
        if (e1(10, 20, 30) == e1(10, 20)) {}
        if (e1(10, 20, 30) == e1(10, 20, 30)) {}
    }

    private native int n1();

    void testNative1()
    {
        if (n1() == n1()) {} //ok
    }

    class A
    {
        int x;
        int y;
    }

    class B
    {
        M m;
    }

    class V
    {
        int x;
        private native void f();
    }

    class M
    {
        V v;
        private void f()
        {
            new Object(); // makes it impure
        }

        private void g()
        {
            new Object(); // makes it impure
            this.v = new V();
        }
    }

    private void g1(B arg)
    {
        new Object(); // makes it impure
    }

    private void g2(B arg)
    {
        new Object(); // makes it impure
        arg.m = null;
    }

    private void g3(B arg)
    {
        new Object(); // makes it impure
        arg.m.v.x = 42;
    }

    private void g4(B arg)
    {
        new Object(); // makes it impure
        arg = null;
    }

    private void g5(B arg)
    {
        new Object(); // makes it impure
        arg.m.v.x += 42;
    }

    private void g6(B arg)
    {
        new Object(); // makes it impure
        ++arg.m.v.x;
    }

    private void g7(B arg)
    {
        new Object(); // makes it impure
        arg.m.v.f();
    }

    private void g8(B[] arg)
    {
        new Object(); // makes it impure
        arg[0].m.v.x = 42;
    }

    private void g9(B arg)
    {
        new Object(); // makes it impure
        g1(arg);
        g1(arg);
        g1(arg);
    }

    private void g10(B arg)
    {
        new Object(); // makes it impure
        g9(arg);
    }

    private void g11(B arg)
    {
        new Object(); // makes it impure
        int x = arg.m.v.x + 1;
    }

    void testReadOnlyArguments1()
    {
        B b = new B();

        b.m.v.x = 1;
        g1(b);
        if (b.m.v.x == 42) {} //@ALWAYS_FALSE

        b.m.v.x = 1;
        g2(b);
        if (b.m.v.x == 42) {} //ok

        b.m.v.x = 1;
        g3(b);
        if (b.m.v.x == 42) {} //ok

        b.m.v.x = 1;
        g4(b);
        if (b.m.v.x == 42) {} //@ALWAYS_FALSE

        b.m.v.x = 1;
        g5(b);
        if (b.m.v.x == 42) {} //ok

        b.m.v.x = 1;
        g6(b);
        if (b.m.v.x == 42) {} //ok

        b.m.v.x = 1;
        g7(b);
        if (b.m.v.x == 42) {} //ok

        B[] arr = new B[] {b};
        arr[0].m.v.x = 1;
        g8(arr);
        if (arr[0].m.v.x == 42) {} //ok

        b.m.v.x = 1;
        g9(b);
        if (b.m.v.x == 42) {} //@ALWAYS_FALSE

        b.m.v.x = 1;
        g10(b);
        if (b.m.v.x == 42) {} //@ALWAYS_FALSE

        b.m.v.x = 1;
        g11(b);
        if (b.m.v.x == 42) {} //@ALWAYS_FALSE
    }

    private void g12(B arg1, B arg2, B arg3)
    {
        new Object(); // makes it impure
        arg3 = arg1;
        arg2.m.v.x = 42;
    }

    void testReadOnlyArguments2()
    {
        B b1 = new B();
        B b2 = new B();
        B b3 = new B();
        b1.m.v.x = 1;
        b2.m.v.x = 1;
        b3.m.v.x = 1;
        g12(b1, b2, b3);
        if (b1.m.v.x == 42) {} //@ALWAYS_FALSE
        if (b2.m.v.x == 42) {} //ok
        if (b3.m.v.x == 42) {} //@ALWAYS_FALSE
    }

    void testReadOnlyTarget1()
    {
        M m = new M();

        m.v.x = 1;
        m.f();
        if (m.v.x == 1) {} //@ALWAYS_TRUE

        m.v.x = 1;
        m.g();
        if (m.v.x == 1) {} //ok
    }
}
