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

    private void throwException(int x)
    {
        if (x > 42)
        {
            throwException();
        }
    }

    private void throwException(int x, Object o)
    {
        if (o == null)
        {
            throw new RuntimeException("error");
        }
    }

    private void throwException(Object o1, Object o2)
    {
        if (o2 != null)
        {
        }
        else
        {
            if (o1 == null)
            {
                throw new RuntimeException("error");
            }
        }
    }

    private void throwException(int i, int j)
    {
        if (i != 42)
        {
        }
        else
        {
            if (j != 42)
            {
                throw new RuntimeException("error");
            }
        }
    }

    private void throwExceptionUnderTrue()
    {
        if (true) //@ALWAYS_TRUE
        {
            throw new RuntimeException("error");
        }
    }

    private void throwExceptionWithReturn1(boolean cond)
    {
        if (cond)
        {
            return;
        }
        throw new RuntimeException("error");
    }

    private void throwExceptionWithReturn2(boolean cond)
    {
        if (true) //@ALWAYS_TRUE
        {
            if (cond)
            {
                return;
            }
            throw new RuntimeException("error");
        }
    }

    private void throwIfLessThanZero(int x)
    {
        if (x < 0)
        {
            throw new RuntimeException("error");
        }
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

    void testThrow3(boolean cond, boolean x)
    {
        if (cond)
        {
            throwException(cond);
        }
        if (cond) {} //@ALWAYS_FALSE
    }

    void testThrow4(boolean cond)
    {
        throwException();
        if (cond) {} //TODO: warn about unreachable code (new checker)
    }

    void testThrow5(boolean cond, boolean x)
    {
        if (x)
        {
            throwException(cond);
        }
        if (x) {} //ok
    }

    void testThrow6(Object o)
    {
        if (o == null)
        {
            throwException(10, o);
        }
        if (o == null) {} //@ALWAYS_FALSE
    }

    void testThrow7(Object o1, Object o2)
    {
        if (o1 == null && o2 == null)
        {
            throwException(o1, o2);
        }
        if (o1 == null && o2 == null) {} //@ALWAYS_FALSE
        if (o1 == null) {} //ok
        if (o2 == null) {} //ok
    }

    void testThrow8(int i, int j)
    {
        if (i == 42 && j != 42)
        {
            throwException(i, j);
        }
        if (i == 42 && j != 42) {} //@ALWAYS_FALSE
    }

    void testThrow9(boolean cond)
    {
        if (cond)
        {
            throwExceptionUnderTrue();
        }
        if (cond) {} //@ALWAYS_FALSE
    }

    void testThrow10(boolean cond)
    {
        throwExceptionWithReturn1(cond);
        if (cond) {} //@ALWAYS_TRUE
    }

    void testThrow11(boolean cond)
    {
        throwExceptionWithReturn1(!cond);
        if (cond) {} //@ALWAYS_FALSE
    }

    void testThrow12(boolean cond)
    {
        throwExceptionWithReturn2(!cond);
        if (cond) {} //@ALWAYS_FALSE
    }

    void testThrow13(int x)
    {
        if (x == 100)
        {
            throwException(x);
        }
        if (x == 100) {} //@ALWAYS_FALSE
    }

    void testThrow14(int x)
    {
        if (x < -10)
        {
            throwIfLessThanZero(x);
        }
        if (x == -12) {} //@ALWAYS_FALSE
        if (x == -2) {} //ok
        if (x == 10) {} //ok
    }

    private void f1(boolean a1, boolean a2)
    {
        if (a1 && a2)
        {
            throw new RuntimeException("error");
        }
    }

    private void f2(boolean b)
    {
        if (!b)
        {
            f1(true, true);
        }
    }

    void testThrow15(boolean cond)
    {
        if (!cond)
        {
            f2(cond);
        }
        if (cond) {} //@ALWAYS_TRUE
    }

    private void throwOne()
    {
        throwException();
    }

    private void throwTwo(boolean b)
    {
        if (b)
        {
            throwOne();
        }
    }

    private void throwThree(boolean b)
    {
        if (!b)
        {
            throwTwo(true);
        }
    }

    void testThrow16(boolean b)
    {
        if (!b)
        {
            throwThree(b);
        }
        if (!b) {} //@ALWAYS_FALSE
    }

    private void throwInsideCatch()
    {
        try
        {
            throw new RuntimeException("err");
        }
        catch (Exception e) {}
    }

    void testThrow17(boolean b)
    {
        if (b)
        {
            throwInsideCatch();
        }
        if (b) {} //ok
    }

    private void throwWithLoop(boolean b, boolean d)
    {
        while (true)
        {
            if (d)
            {
                break;
            }
            if (b)
            {
                throw new RuntimeException("err");
            }
        }
    }

    void testThrow18(boolean b)
    {
        if (b)
        {
            throwWithLoop(b, true);
        }
        if (b) {} //ok
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

    private int x;
    private Integer y;

    public int getX()
    {
        return x;
    }

    public Integer getY()
    {
        return y;
    }

    public int get()
    {
        return 42;
    }

    void testGetter1()
    {
        x = 42;
        if (getX() == 42) {} //@ALWAYS_TRUE

        y = new Integer(42);
        if (getY() == 42); //@ALWAYS_TRUE
    }

    void testGetter2()
    {
        if (getX() == getX()) {} //@ALWAYS_TRUE
    }

    void testGetName()
    {
        if (get() == get()) {} //ok
    }

    public void setX(int arg)
    {
        x = arg;
    }

    public void setY(Integer arg)
    {
        y = arg;
    }

    public void set(int arg)
    {
        x = arg * y;
    }

    void testSetter1()
    {
        setX(42);
        if (x == 42) {} //@ALWAYS_TRUE

        setY(42);
        if (y == 42) {} //@ALWAYS_TRUE

        setY(new Integer(142));
        if (y == 142) {} //@ALWAYS_TRUE
    }

    void testSetName()
    {
        set(42); //ok
    }
}
