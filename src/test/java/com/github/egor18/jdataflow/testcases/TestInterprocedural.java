package com.github.egor18.jdataflow.testcases;

public class TestInterprocedural
{
    public class TestPure
    {
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
    }

    public class TestThrowEffect
    {
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

        private void throwInsideCatchIfTwo(int x)
        {
            switch (x)
            {
                case 1:
                    break;
                case 2:
                    throw new RuntimeException("error");
                case 3:
                    break;
            }
        }

        public boolean m;

        private void throwOneOfExceptions1()
        {
            if (m)
            {
                throw new RuntimeException("error 1");
            }
            else
            {
                throw new RuntimeException("error 2");
            }
        }

        public int selector;

        private void throwOneOfExceptions2()
        {
            switch (selector)
            {
                case 1:
                    throw new RuntimeException("error 1");
                case 2:
                    throw new RuntimeException("error 2");
                case 3:
                    throw new RuntimeException("error 3");
                default:
                    throw new RuntimeException("error 4");
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
            if (cond) {} //@ALWAYS_FALSE
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

        void testThrow19(int i)
        {
            if (i == 2)
            {
                throwInsideCatchIfTwo(i);
            }
            if (i == 2) {} //@ALWAYS_FALSE
       }

       void testThrow20(boolean a)
       {
           throwOneOfExceptions1();
           if (a) {} //@ALWAYS_FALSE
       }

       void testThrow21(boolean a)
       {
           throwOneOfExceptions2();
           if (a) {} //@ALWAYS_FALSE
       }
    }

    public class TestParametersOnlyDependant
    {
        private void f1(boolean arg)
        {
            boolean c = arg && true; //@ALWAYS_TRUE
            if (c)
            {
                throw new RuntimeException("error");
            }
        }

        void testParameterOnlyDependant1(boolean a)
        {
            f1(a);
            if (a) {} //@ALWAYS_FALSE
        }

        public boolean m;

        private void f2(boolean arg)
        {
            boolean c = arg && m;
            if (c)
            {
                throw new RuntimeException("error");
            }
        }

        void testParameterOnlyDependant2(boolean a)
        {
            f2(a);
            if (a) {} //ok
        }

        private void f3(int arg)
        {
            int a1 = arg + 1;
            int a2 = a1 + 2;
            int a3 = a2 + 3;
            if (a3 > 10)
            {
                throw new RuntimeException("error");
            }
        }

        void testParameterOnlyDependant3(boolean a)
        {
            f3(2);
            if (a) {} //ok

            f3(30);
            if (a) {} //@ALWAYS_FALSE
        }

        private void f4(boolean arg)
        {
            boolean c = true;
            if (c) //@ALWAYS_TRUE
            {
                throw new RuntimeException("error");
            }
        }

        void testParameterOnlyDependant4(boolean a)
        {
            f4(true);
            if (a) {} //@ALWAYS_FALSE
        }

        public static final boolean cm = true;

        private void f5(boolean arg)
        {
            if (cm) //@ALWAYS_TRUE
            {
                throw new RuntimeException("error");
            }
        }

        void testParameterOnlyDependant5(boolean a)
        {
            f5(true);
            if (a) {} //@ALWAYS_FALSE
        }
    }

    public class TestRecursion
    {
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
    }

    public class TestEllipsis
    {
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
    }

    public class TestNative
    {
        private native int n1();

        void testNative1()
        {
            if (n1() == n1()) {} //ok
        }
    }

    public class TestReadOnly
    {
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

    public class TestGetterSetter
    {
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

    public class TestParameterDereferenceEffect
    {
        private void f1(boolean cond, Object o)
        {
            if (cond)
            {
                int z = o.hashCode();
            }
        }

        private void f2(int x, int y, Object o)
        {
            if (x > 10)
            {
                f1(x > 20, o);
            }
        }

        private void f3(int n, Object o)
        {
            if (n != 1234)
            {
                f2(n, 0, o);
            }
        }

        public boolean m;

        private void f4(Object o)
        {
            if (m)
            {
                int z = o.hashCode();
            }
        }

        private void f5(Object o)
        {
            if (true) //@ALWAYS_TRUE
            {
                int z = o.hashCode();
            }
        }

        private void f6(String[] o)
        {
            int z = o.length;
        }

        private void f7(String[] o)
        {
            String z = o[42];
        }

        private void f8(String[] o)
        {
            o[42] = "hello";
        }

        private void f9(Object x, Object y)
        {
            if (x == null)
            {
                int a = y.hashCode();
            }
        }

        private void f10(Object a, Object b, Object c)
        {
            f9(a, c);
            if (a == null) {} //ok
            if (c == null) {} //ok
            if (a == null && c == null) {} //@ALWAYS_FALSE
        }

        private void f11(Object a, Object b, Object c)
        {
            int z = b.hashCode();
        }

        private void f12(Object a, Object b, Object c)
        {
            f11(c, c, a);
        }

        private void f13(Object a, Object b, Object c)
        {
            f12(c, a, b);
        }

        private void f14(Object a, Object b, Object c)
        {
            f13(a, b, c);
        }

        private void f15(Object x)
        {
            Object x1 = x;
            Object x2 = x1;
            Object x3 = x2;
            int a = x3.hashCode();
        }

        private void f16(Object x)
        {
            int z = x.hashCode();
        }

        private void f17(boolean c, Object o1, Object o2)
        {
            Object t = c ? o1 : o2;
            int z = t.hashCode();
        }

        private void f18(Object x)
        {
            x = new Object();
            int z = x.hashCode();
        }

        private void f19(Object x)
        {
            if (m)
            {
                int z = x.hashCode();
            }
            else
            {
                int w = x.hashCode();
            }
        }

        private void f20(Object x, boolean c)
        {
            if (c)
            {
                int z = x.hashCode();
            }
            else
            {
                int w = x.hashCode();
            }
        }

        private void f21(Object x, Object y)
        {
            if (m)
            {
                int z = x.hashCode();
            }
            else
            {
                int w = y.hashCode();
            }
        }

        private void f22(Object x)
        {
            Object y = new Object();
            if (m)
            {
                int z = x.hashCode();
            }
            else
            {
                int w = y.hashCode();
            }
        }

        private void f23(boolean c, Object x)
        {
            Object y = new Object();
            if (c)
            {
                int z = x.hashCode();
            }
            else
            {
                int w = y.hashCode();
            }
        }

        private void testParameterDereferenceEffect1(Object o)
        {
            f1(false, o);
            if (o == null) {} //ok

            f1(true, o);
            if (o == null) {} //@ALWAYS_FALSE
        }

        private void testParameterDereferenceEffect2(Object o)
        {
            f2(0, 0, o);
            if (o == null) {} //ok

            f2(15, 0, o);
            if (o == null) {} //ok

            f2(30, 0, o);
            if (o == null) {} //@ALWAYS_FALSE
        }

        private void testParameterDereferenceEffect3(Object o)
        {
            f3(1234, o);
            if (o == null) {} //ok

            f3(1235, o);
            if (o == null) {} //@ALWAYS_FALSE
        }

        private void testParameterDereferenceEffect4(Object o)
        {
            f4(o);
            if (o == null) {} //ok
        }

        private void testParameterDereferenceEffect5(Object o)
        {
            f5(o);
            if (o == null) {} //@ALWAYS_FALSE
        }

        private void testParameterDereferenceEffect6(String[] o)
        {
            f6(o);
            if (o == null) {} //@ALWAYS_FALSE
        }

        private void testParameterDereferenceEffect7(String[] o)
        {
            f7(o);
            if (o == null) {} //@ALWAYS_FALSE
        }

        private void testParameterDereferenceEffect8(String[] o)
        {
            f8(o);
            if (o == null) {} //@ALWAYS_FALSE
        }

        private void testParameterDereferenceEffect9(Object o1, Object o2, Object o3)
        {
            f9(o1, o2);
            if (o1 == null) {} //ok
            if (o2 == null) {} //ok
            if (o3 == null) {} //ok
        }

        private void testParameterDereferenceEffect10(Object o1, Object o2, Object o3)
        {
            f10(null, o2, o3);
            if (o1 == null) {} //ok
            if (o2 == null) {} //ok
            if (o3 == null) {} //@ALWAYS_FALSE
        }

        private void testParameterDereferenceEffect11(Object o1, Object o2, Object o3)
        {
            f10(o1, o2, o3);
            if (o1 == null) {} //ok
            if (o2 == null) {} //ok
            if (o3 == null) {} //ok
        }

        private void testParameterDereferenceEffect12(Object o1, Object o2, Object o3)
        {
            f14(o3, o1, o2);
            if (o1 == null) {} //@ALWAYS_FALSE
            if (o2 == null) {} //ok
            if (o3 == null) {} //ok
        }

        private void testParameterDereferenceEffect13(Object o)
        {
            f15(o);
            if (o == null) {} //@ALWAYS_FALSE
        }

        private void testParameterDereferenceEffect14(Object o)
        {
            if (o != null)
            {
                f16(o);
            }
            if (o == null) {} //ok
        }

        void testParameterDereferenceEffect15(boolean a)
        {
            f17(true,null, null); //@NULL_DEREFERENCE_INTERPROCEDURAL
            if (a) {} //@ALWAYS_FALSE
        }

        void testParameterDereferenceEffect16(boolean a)
        {
            f17(false, null, null); //@NULL_DEREFERENCE_INTERPROCEDURAL
            if (a) {} //@ALWAYS_FALSE
        }

        void testParameterDereferenceEffect17(boolean a, Object o)
        {
            f17(true, o, null);
            if (a) {} //ok
        }

        void testParameterDereferenceEffect18(boolean a, Object o)
        {
            f17(true,null, o); //@NULL_DEREFERENCE_INTERPROCEDURAL
            if (a) {} //@ALWAYS_FALSE
        }

        void testParameterDereferenceEffect19(boolean a, Object o)
        {
            f17(false, o, null); //@NULL_DEREFERENCE_INTERPROCEDURAL
            if (a) {} //@ALWAYS_FALSE
        }

        void testParameterDereferenceEffect20(boolean a, Object o)
        {
            f17(false,null, o);
            if (a) {} //ok
        }

        void testParameterDereferenceEffect21(Object o)
        {
            f18(o);
            if (o == null) {} //ok
        }

        void testParameterDereferenceEffect22(Object o)
        {
            f19(o);
            if (o == null) {} //@ALWAYS_FALSE
        }

        void testParameterDereferenceEffect23(Object o)
        {
            f20(o, false);
            if (o == null) {} //@ALWAYS_FALSE
        }

        void testParameterDereferenceEffect24(Object o)
        {
            f20(o, true);
            if (o == null) {} //@ALWAYS_FALSE
        }

        void testParameterDereferenceEffect25(Object o1, Object o2)
        {
            f21(o1, o2);
            if (o1 == null && o2 == null) {} //@ALWAYS_FALSE
        }

        void testParameterDereferenceEffect26(Object o)
        {
            f22(o);
            if (o == null) {} //ok
        }

        void testParameterDereferenceEffect27(Object o)
        {
            f23(true, o);
            if (o == null) {} //@ALWAYS_FALSE
        }

        void testParameterDereferenceEffect28(Object o)
        {
            f23(false, o);
            if (o == null) {} //ok
        }
    }

    public class TestInterproceduralNullDereference
    {
        private void f1(Object x)
        {
            int z = x.hashCode();
        }

        private void f2(int i, Object x)
        {
            if (i < 10)
            {
                f1(x);
            }
        }

        private void f3(int i, Object x)
        {
            if (i < 4)
            {
                f2(i, x);
            }
        }

        void testInterproceduralNullDereference1()
        {
            f1(null); //@NULL_DEREFERENCE_INTERPROCEDURAL
        }

        void testInterproceduralNullDereference2(Object o)
        {
            f2(5, o); //ok
            f2(5, null); //@NULL_DEREFERENCE_INTERPROCEDURAL
        }

        void testInterproceduralNullDereference3(Object o)
        {
            f2(15, o); //ok
            f2(15, null); //ok
        }

        void testInterproceduralNullDereference4(Object o)
        {
            if (o == null)
            {
                f1(o); //@NULL_DEREFERENCE_INTERPROCEDURAL
            }

            f1(null); //@NULL_DEREFERENCE_INTERPROCEDURAL
        }

        void testInterproceduralNullDereference5(int i, Object o)
        {
            f2(5, o);
            f2(10, o);
            f2(i, o); //ok

            if (i == 5)
            {
                f2(i, null); //@NULL_DEREFERENCE_INTERPROCEDURAL
            }

            if (i == 10)
            {
                f2(i,  null); //ok
            }
        }

        void testInterproceduralNullDereference6(Object x)
        {
            f3(3, x); //ok
            f3(8, x); //ok
            f3(3, null); //@NULL_DEREFERENCE_INTERPROCEDURAL
            f3(8, null); //ok
        }
    }
}
