package com.github.egor18.jdataflow.testcases;

public class TestResultIgnored
{
    private int getX()
    {
        return 42;
    }

    private void doSomething()
    {
    }

    void testResultIgnored1()
    {
        getX(); //@RESULT_IGNORED
        Math.min(10, 20); //@RESULT_IGNORED
        doSomething(); //ok

        int x = getX(); //ok
        int y = Math.min(10, 20); //ok
    }
}
