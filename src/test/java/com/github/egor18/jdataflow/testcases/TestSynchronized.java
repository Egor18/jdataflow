package com.github.egor18.jdataflow.testcases;

public class TestSynchronized
{
    public int count;
    public volatile Object object;

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
}
