package com.github.egor18.jdataflow.testcases;

public class TestSynchronized<T>
{
    public int count;
    public volatile Object object;
    private volatile T genObject;

    void testSynchronized1()
    {
        count = 0;
        int x = 0;
        synchronized (this)
        {
            if (count == 0) {} //ok
            if (x == 0) {} //@ALWAYS_TRUE
            count = 5;
        }
        if (count == 5) {} //@ALWAYS_TRUE
        if (x == 0) {} //@ALWAYS_TRUE
    }

    void testSynchronized2()
    {
        if (object == null)
        {
            synchronized (this)
            {
                if (object == null) //ok
                {
                    object = new Object();
                }
            }
        }
    }

    void testSynchronized3()
    {
        if (this.genObject == null)
        {
            synchronized (this)
            {
                if (this.genObject == null) {} //ok
            }
        }
    }

    class A
    {
        public int x;
    }

    void testSynchronized4(A a)
    {
        if (a.x != 42)
        {
            synchronized (a)
            {
                if (a.x != 42) {} //ok
            }
        }
    }
}
