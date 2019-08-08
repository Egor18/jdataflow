package com.github.egor18.jdataflow.testcases;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class TestInvocation
{
    private class C1 { int x; }
    private class C2 { int x; native void unknownFunc(); }
    private class C3 { int x; public int getX() { return x; }}
    private class C4 { C3 x; public C3 getX() { return x; }}
    private C4 getC4() { return new C4(); }
    private native int unknownFunc1();
    private native void unknownFunc2(C1 arg);
    private native void unknownFunc3(Integer arg);
    private C1 unknownFunc4() { return new C1(); }
    private native static void unknownFunc5(int i);
    public class M { int x; public native void f(); }
    public class N { int x; public native void f(); }
    int x;
    Object o;
    M p;

    void testInvocation1()
    {
        C1 obj = new C1();
        obj.x = 12;

        unknownFunc2(obj);

        if (obj == null) {} //@ALWAYS_FALSE
        if (obj.x == 12) {} //ok
    }

    void testInvocation2()
    {
        Integer x = new Integer(42);

        unknownFunc3(x); // Integer is immutable, so the value remains

        if (x == null) {} //@ALWAYS_FALSE
        if (x == 42) {} //@ALWAYS_TRUE
    }

    void testInvocation3()
    {
        int x = getC4().getX().getX();
    }

    void testInvocation4()
    {
        if (this.unknownFunc1() == this.unknownFunc1()) {} //ok
    }

    void testInvocation5()
    {
        C2 obj = new C2();
        obj.x = 42;
        if (obj.x == 42) {} //@ALWAYS_TRUE
        obj.unknownFunc();
        if (obj.x == 42) {} //ok
    }

    void testInvocationTarget1()
    {
        int a = unknownFunc4().x;
        unknownFunc4().x = 42;
        if (unknownFunc4().x == 42) {} //ok
    }

    void testInvocationTarget2(int i)
    {
        TestInvocation.unknownFunc5(i);
    }

    void testIndirectModification1()
    {
        x = 1;
        M m = new M()
        {
            @Override
            public void f()
            {
                x = 42;
            }
        };
        m.f();
        if (x == 1) {} //ok
    }

    void testIndirectModification2()
    {
        o = new Object();
        M m = new M()
        {
            @Override
            public void f()
            {
                o = null;
            }
        };
        m.f();
        if (o == null) {}
    }

    void testIndirectModification3()
    {
        p = new M();
        p.x = 1;
        M m = new M()
        {
            @Override
            public void f()
            {
                p.x = 42;
            }
        };
        m.f();
        if (p == null) {}
        if (p.x == 1) {}
    }

    void testIndirectModification4()
    {
        int z = 1;
        M m = new M()
        {
            @Override
            public void f()
            {
                // z = 42; // impossible (z should be effectively final)
            }
        };
        m.f();
        if (z == 1) {} //@ALWAYS_TRUE
    }

    void testIndirectModification5()
    {
        M p = new M();
        p.x = 1;
        M m = new M()
        {
            @Override
            public void f()
            {
                // p = null; // impossible (p should be effectively final)
                p.x = 42;
            }
        };
        m.f();
        if (p == null) {} //@ALWAYS_FALSE
        if (p.x == 1) {}
    }

    void testIndirectModification6(int arg)
    {
        arg = 1;
        M m = new M()
        {
            @Override
            public void f()
            {
                // arg = 42; // impossible (arg should be effectively final)
            }
        };
        m.f();
        if (arg == 1) {} //@ALWAYS_TRUE
    }

    void testIndirectModification7(M arg)
    {
        arg.x = 1;
        M m = new M()
        {
            @Override
            public void f()
            {
                arg.x = 42;
            }
        };
        m.f();
        if (arg.x == 1) {}
    }

    void testIndirectModification8(M m)
    {
        x = 1;
        m.f();
        if (x == 1) {} //ok (it's possible to pass such m that changes field x)
    }

    void testIndirectModification9(M m)
    {
        N p = new N();
        p.x = 1;
        m.f();
        if (p.x == 1) {} //@ALWAYS_TRUE

    }

    void testIndirectModification10(M m)
    {
        M p = new M();
        p.x = 1;
        m.f();
        if (p.x == 1) {} //FIXME: should be always true here
        // Explanation: Right we assume that m could point to any memory address (including local variable p)
    }

    void testIndirectModification11()
    {
        int[] a = new int[1];
        a[0] = 1;
        int[] b = new int[1];
        b[0] = 1;
        class Local
        {
            private void run()
            {
                a[0] = 42;
            }
        }
        if (a[0] == 1) {} //@ALWAYS_TRUE
        if (b[0] == 1) {} //@ALWAYS_TRUE
        Local loc = new Local();
        loc.run();
        if (a[0] == 1) {} //ok
        if (b[0] == 1) {} //@ALWAYS_TRUE
    }

    void testIndirectModification12(N m)
    {
        M p = new M();
        p.x = 1;

        m.f();
        if (p.x == 1) {} //@ALWAYS_TRUE

        M x = new M()
        {
            @Override
            public void f()
            {
                // this indirect modification goes after the m.f() invocation so it could not change p.x there
                p.x = 42;
            }
        };
    }

    void testIndirectModifications13(List<Integer> elements)
    {
        int[] arr = {1, 2, 3};
        if (arr[0] == 1) {} //@ALWAYS_TRUE
        if (arr[1] == 2) {} //@ALWAYS_TRUE
        if (arr[2] == 3) {} //@ALWAYS_TRUE
        elements.forEach(e -> arr[0] = e);
        if (arr[0] == 1) {} //ok
        if (arr[1] == 2) {} //ok
        if (arr[2] == 3) {} //ok
    }

    void testIndirectModifications14()
    {
        M m1 = new M();
        m1.x = 1;
        M m2 = new M();
        m2.x = 2;
        Function<Integer, Integer> func = (Integer a) -> { m2.x = 42; return 0; };
        func.apply(10);
        if (m1.x == 1) {} //@ALWAYS_TRUE
        if (m2.x == 42) {} //ok
    }

    class TestSuperReset
    {
        class Base
        {
            public int x;
            public void f() {}
        }

        class Derived extends Base
        {
            public int x;
            public native void g();

            @Override
            public void f()
            {
                this.x = 1;
                super.x = 1;
                g();
                if (super.x == 1) {} //ok
                if (this.x == 1) {} //ok
            }
        }
    }
}
