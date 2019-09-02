package com.github.egor18.jdataflow.testcases;

public class TestAnonymousClass
{
    public class C
    {
        public native int f();
    }

    int y;
    int z;

    void testAnonymousClass1()
    {
        C c = new C()
        {
            @Override
            public int f()
            {
                return 42;
            }
        };
    }

    void testAnonymousClass2()
    {
        final int a = 0;
        z = 0;
        C c1 = new C()
        {
            @Override
            public int f()
            {
                if (z == 0) {} //ok
                if (a == 0) {} //@ALWAYS_TRUE
                z = 42;
                return 42;
            }
        };

        z = 1;
        int u = c1.f();
        if (z == 42) {}

        C c2 = new C()
        {
            @Override
            public int f()
            {
                if (z == 0) {} //ok
                return 42;
            }
        };
    }

    void testAnonymousClass3(C arg)
    {
        z = 0;
        C c = new C()
        {
            @Override
            public int f()
            {
                return 42;
            }
        };

        z = 1;
        int u = arg.f();
        if (u == 42) {}
        if (z == 42) {}
    }

    void testAnonymousClass4()
    {
        z = 0;
        y = 0;
        final int i = 42;
        C c = new C()
        {
            @Override
            public int f()
            {
                z = 1 + i;
                return 42;
            }
        };
        if (z == 0) {} //@ALWAYS_TRUE
        if (z == 1) {} //@ALWAYS_FALSE
        if (y == 0) {} //@ALWAYS_TRUE
        if (y == 1) {} //@ALWAYS_FALSE
        c.f();
        if (z == 0) {}
        if (z == 1) {}
        if (y == 0) {}
        if (y == 1) {}
    }

    class D { int x; }
    void testAnonymousClass5()
    {
        D d = new D();
        d.x = 42;
        C c = new C()
        {
            @Override
            public int f()
            {
                d.x = 1; //ok
                return 42;
            }
        };
        if (d == null) {} //@ALWAYS_FALSE
        if (d.x == 42) {} //@ALWAYS_TRUE
        c.f();
        if (d == null) {} //@ALWAYS_FALSE
        if (d.x == 42) {}
    }

    Object object = new Object()
    {
        @Override
        public int hashCode()
        {
            return 42;
        }
    };

    int a;
    final int b = 142;
    class X { int x; }
    void testAnonymousClass6()
    {
        a = 0;
        int j = 42;
        final int i = 42;
        int[] arr = {1, 2, 3};
        X q = new X();
        q.x = 42;
        C c = new C()
        {
            @Override
            public int f()
            {
                if (a == 100) {} //ok
                if (b == 142) {} //@ALWAYS_TRUE
                if (i == 42) {} //@ALWAYS_TRUE
                if (j == 142) {} //@ALWAYS_FALSE
                if (arr[1] == 2) {} //ok
                if (q.x == 42) {} //ok
                return 42;
            }
        };
        if (a == 0) {} //@ALWAYS_TRUE
        if (a == 1) {} //@ALWAYS_FALSE
        c.f();
        if (a == 0) {}
        if (a == 1) {}
    }
}
