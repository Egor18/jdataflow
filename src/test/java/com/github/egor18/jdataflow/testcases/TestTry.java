package com.github.egor18.jdataflow.testcases;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class TestTry
{
    void testTry1()
    {
        int a = 0;
        int b = 0;
        int c = 0;
        try
        {
            a = 1;
        }
        catch (RuntimeException e)
        {
            a = 2;
            b = 1;
        }
        if (a == 0) {} //ok
        if (a == 1) {} //ok
        if (a == 2) {} //ok
        if (b == 1) {} //ok
        if (c == 0) {} //@ALWAYS_TRUE
    }

    void testTry2()
    {
        int a = 0;
        int c = 0;
        try
        {
            a = 1;
        }
        catch (RuntimeException e)
        {
            a = 2;
        }
        finally
        {
            a = 42;
            c = 42;
        }
        if (a == 0) {} //@ALWAYS_FALSE
        if (a == 1) {} //@ALWAYS_FALSE
        if (a == 2) {} //@ALWAYS_FALSE
        if (a == 42) {} //@ALWAYS_TRUE
        if (c == 42) {} //@ALWAYS_TRUE
    }

    void testTry3()
    {
        int a = 0;
        int b = 0;
        try
        {
            a = 1;
        }
        catch (ArithmeticException e)
        {
            b = 1;
        }
        catch (RuntimeException e)
        {
        }
        if (a == 0) {} //ok
        if (b == 0) {} //ok
    }

    void testTry4()
    {
        int a = 5;
        try
        {
            if (a == 5) {} //@ALWAYS_TRUE
        }
        catch (RuntimeException e)
        {
        }
    }

    void testTry5(boolean cond)
    {
        try
        {
            while (true)
            {
                if (cond)
                {
                    return;
                }
            }
        }
        finally
        {
            if (true) {} //@ALWAYS_TRUE
            if (false) {} //@ALWAYS_FALSE
            if (cond) {} //@ALWAYS_TRUE
        }
    }

    public native void unknownFunc();

    void testTry6()
    {
        boolean interrupted = false;
        try
        {
            while (true)
            {
                try
                {
                    unknownFunc();
                    return;
                }
                catch (Exception e)
                {
                    interrupted = true;
                }
            }
        }
        finally
        {
            if (true) {} //@ALWAYS_TRUE
            if (false) {} //@ALWAYS_FALSE
            if (interrupted) {} //ok
        }
    }

    void testTry7(boolean cond)
    {
        try
        {
            while (true)
            {
                if (cond)
                {
                    throw new RuntimeException();
                }
            }
        }
        finally
        {
            if (true) {} //@ALWAYS_TRUE
            if (false) {} //@ALWAYS_FALSE
            if (cond) {} //@ALWAYS_TRUE
        }
    }

    void testTry8()
    {
        boolean interrupted = false;
        try
        {
            while (true)
            {
                try
                {
                    unknownFunc();
                    throw new RuntimeException();
                }
                catch (Exception e)
                {
                    interrupted = true;
                }
            }
        }
        finally
        {
            if (interrupted) {} //unreachable //@ALWAYS_FALSE
        }
    }

    void testTry9()
    {
        boolean interrupted = false;
        while (true)
        {
            try
            {
                unknownFunc();
                break;
            }
            catch (Exception e)
            {
                interrupted = true;
            }
            finally
            {
                if (true) {} //@ALWAYS_TRUE
                if (false) {} //@ALWAYS_FALSE
                if (interrupted) {} //ok
            }
        }
    }

    void testTry10()
    {
        int a = 1;
        int b = 1;

        try
        {
            a = 2;
            unknownFunc();
            b = 2;
        }
        catch (IllegalArgumentException e)
        {
            a = 3;
            b = 3;
        }
        catch (ArithmeticException e)
        {
            a = 4;
            b = 4;
        }

        if (a == 1) {}
        if (b == 1) {}
        if (a == 2) {}
        if (b == 2) {}
        if (a == 3) {}
        if (b == 3) {}
        if (a == 4) {}
        if (b == 4) {}
        if (a == 5) {} //@ALWAYS_FALSE
        if (a == 5) {} //@ALWAYS_FALSE
        if (a == 2 && b == 3) {} //@ALWAYS_FALSE
        if (a == 3 && b == 4) {} //@ALWAYS_FALSE
    }

    void testTry11()
    {
        int a = 1;
        int b = 1;

        try
        {
            unknownFunc();
            a = 2;
            unknownFunc();
            b = 2;
            unknownFunc();
        }
        catch (IllegalArgumentException e)
        {
            b = 3;
        }
        catch (ArithmeticException e)
        {
            a = 4;
        }

        if (a == 1) {}
        if (b == 1) {}
        if (a == 2) {}
        if (b == 2) {}
        if (a == 2 && b == 3) {}
        if (a == 4 && b == 3) {} //@ALWAYS_FALSE
        if (a == 4 && b == 2) {}
    }

    void testTry12()
    {
        boolean isEmpty = true;
        try
        {
            unknownFunc();
            isEmpty = false;
            unknownFunc();
            return;
        }
        catch (RuntimeException e)
        {
            if (isEmpty) {} //ok
        }
    }

    void testTry13()
    {
        int a = 1;
        int b = 1;

        try
        {
            unknownFunc();
            a = 2;
            unknownFunc();
            b = 2;
        }
        catch (IllegalArgumentException e)
        {
        }
        catch (RuntimeException e)
        {
        }

        if (a == 2 && b == 1) {} //ok
        if (a == 2 && b == 2) {} //ok
        if (a == 3) {} //@ALWAYS_FALSE
        if (a != 1 && a != 2) {} //@ALWAYS_FALSE
        if (b != 1 && b != 2) {} //@ALWAYS_FALSE
    }

    void testTry14()
    {
        int a = 1;
        int b = 1;

        try
        {
            unknownFunc();
            a = 2;
            unknownFunc();
            b = 2;
            unknownFunc();
        }
        catch (ArithmeticException e)
        {
            a = 8;
        }
        catch (Exception e)
        {
            b = 8;
        }

        if (a == 8 && b == 8) {} //@ALWAYS_FALSE
    }

    void testTryWithResource1(String path)
    {
        int a = 0;
        int b = 0;
        int c = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(path)))
        {
            if (br == null) {} //@ALWAYS_FALSE
            a = 5;
        }
        catch (FileNotFoundException e)
        {
            b = 5;
        }
        catch (IOException e)
        {
        }
        finally
        {
            b = 55;
            c = 55;
        }
        if (a == 0) {}
        if (a == 5) {}
        if (a == 55) {} //@ALWAYS_FALSE
        if (b == 55) {} //@ALWAYS_TRUE
        if (c == 55) {} //@ALWAYS_TRUE
    }

    void testThrow1()
    {
        boolean cond = true;
        if (cond) //@ALWAYS_TRUE
        {
            throw new RuntimeException();
        }
        if (cond) {} //@ALWAYS_FALSE
    }

    void testThrow2()
    {
        boolean cond = true;
        try
        {
            throw new RuntimeException();
        }
        catch (RuntimeException e)
        {
        }
        if (cond) {} //@ALWAYS_TRUE
    }
}
