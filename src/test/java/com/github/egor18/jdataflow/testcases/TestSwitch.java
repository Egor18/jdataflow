package com.github.egor18.jdataflow.testcases;

public class TestSwitch
{
    private enum SomeEnum { One, Two, Three }

    void testSwitch1(int x)
    {
        switch (x)
        {
            case 1:
                if (x == 1) {} //@ALWAYS_TRUE
                return;
            case 2:
                if (x == 1) {} //@ALWAYS_FALSE
                if (x == 2) {} //@ALWAYS_TRUE
                return;
            case 3:
            case 4:
            case 5:
                if (x == 3 || x == 4 || x == 5) {} //@ALWAYS_TRUE
                return;
        }

        if (x == 1) {} //@ALWAYS_FALSE
        if (x == 2) {} //@ALWAYS_FALSE
        if (x == 4) {} //@ALWAYS_FALSE
    }

    void testSwitch2(int x)
    {
        switch (x)
        {
            case 1:
                x = 2;
            case 2:
                x = 5;
            case 5:
                x = 10;
        }

        if (x == 1) {} //@ALWAYS_FALSE
        if (x == 2) {} //@ALWAYS_FALSE
        if (x == 5) {} //@ALWAYS_FALSE
    }

    void testSwitch3(int x)
    {
        switch (x)
        {
            case 1:
                if (x == 1) {} //@ALWAYS_TRUE
                break;
            case 2:
                if (x == 1) {} //@ALWAYS_FALSE
                if (x == 2) {} //@ALWAYS_TRUE
                break;
            case 3:
            case 4:
            case 5:
                if (x == 2) {} //@ALWAYS_FALSE
                if (x == 3 || x == 4 || x == 5) {} //@ALWAYS_TRUE
                break;
        }

        if (x == 1) {} //ok
        if (x == 2) {} //ok
        if (x == 4) {} //ok
    }

    void testSwitch4(int x, int y)
    {
        switch (x)
        {
            case 1:
            case 2:
            case 3:
                break;
            case 4:
            case 5:
                if (x == 1) {} //@ALWAYS_FALSE
                switch (y)
                {
                    case 1:
                        if (x == 1) {} //@ALWAYS_FALSE
                        if (x == 2) {} //@ALWAYS_FALSE
                        if (x == 3) {} //@ALWAYS_FALSE
                        if (x == 4) {}
                        if (x == 5) {}
                        break;
                }
                if (x == 1) {} //@ALWAYS_FALSE
        }

        if (x == 1) {} //ok
        if (x == 2) {} //ok
        if (x == 4) {} //ok
        if (y == 1) {} //ok
    }

    void testSwitch5(int x)
    {
        switch (x) {}
    }

    void testSwitch6(int x, int y)
    {
        switch (x)
        {
            case 1:
                y = 2;
                break;
            case 2:
                y = 4;
                break;
        }

        if (x == 1)
        {
            if (y == 2) {} //@ALWAYS_TRUE
        }

        if (x == 2)
        {
            if (y == 4) {} //@ALWAYS_TRUE
        }
    }

    void testSwitch7(int x, int y)
    {
        y = 42;
        switch (x)
        {
            case 1:
            case 2:
                return;
            case 3:
                y = 10;
                break;
            case 4:
            case 5:
                y = 20;
                return;
        }

        if (x == 1) {} //@ALWAYS_FALSE
        if (x == 2) {} //@ALWAYS_FALSE
        if (x == 3) {} //ok
        if (x == 4) {} //@ALWAYS_FALSE
        if (x == 5) {} //@ALWAYS_FALSE
        if (y == 10) {} //ok
        if (y == 20) {} //@ALWAYS_FALSE
    }

    void testSwitch8(int x, int y)
    {
        y = 0;
        switch (x)
        {
            case 1:
            case 2:
            case 3:
                y = 10;
                if (x == 2)
                {
                    return;
                }
            case 4:
            case 5:
                y = 20;
                break;
        }

        if (x == 1) {} //ok
        if (x == 2) {} //@ALWAYS_FALSE
        if (x == 3) {} //ok
        if (y == 0) {} //ok
        if (y == 10) {} //@ALWAYS_FALSE
        if (y == 20) {} //ok
    }

    void testSwitch9(int x, int y)
    {
        switch (x)
        {
            case 1:
                y = 10;
                break;
            case 2:
                y = 20;
                break;
            default:
                if (x == 1) {} //@ALWAYS_FALSE
                if (x == 2) {} //@ALWAYS_FALSE
                y = 30;
        }

        if (x != 1 && x != 2)
        {
            if (y == 30) {} //@ALWAYS_TRUE
        }

        if (y == 10) {} //ok
        if (y == 20) {} //ok
        if (y == 30) {} ///ok
    }

    void testSwitch10(int x, int y)
    {
        switch (x)
        {
            case 1:
                y = 2;
                break;
            case 2:
                y = 4;
                return;
            case 3:
                y = 8;
                break;
            default:
                y = 64;
                break;
        }

        if (x == 1)
        {
            if (y == 2) {} //@ALWAYS_TRUE
        }
        else if (x == 2) //@ALWAYS_FALSE
        {
            if (y == 4) {}
        }
        else if (x == 3)
        {
            if (y == 8) {} //@ALWAYS_TRUE
        }
        else
        {
            if (y == 64) {} //@ALWAYS_TRUE
        }
    }

    void testSwitch11(int x, int y)
    {
        y = 0;
        switch (x)
        {
            case 1:
                y = 10;
            case 2:
                y = 20;
                break;
            default:
                return;
        }

        if (y == 0) {} //@ALWAYS_FALSE
        if (y == 10) {} //@ALWAYS_FALSE
        if (y == 20) {} //@ALWAYS_TRUE
        if (x == 1 || x == 2) {}  //@ALWAYS_TRUE
    }

    void testSwitch12(int x)
    {
        int y = 5;
        switch (x)
        {
            default:
                if (y == 5) {} //@ALWAYS_TRUE
                return;
        }
    }

    void testSwitch13()
    {
        int x = 0;
        int a = 0;
        switch (x)
        {
            case 0: //@ALWAYS_TRUE
                a += 1;
            case 1: //@ALWAYS_FALSE
                a += 1;
            case 3: //@ALWAYS_FALSE
                a += 2;
            default:
                break;
        }
        if (a == 4) {} //@ALWAYS_TRUE

        x = 1;
        a = 0;
        switch (x)
        {
            case 0: //@ALWAYS_FALSE
                a += 1;
            case 1: //@ALWAYS_TRUE
                a += 1;
            case 3: //@ALWAYS_FALSE
                a += 2;
            default:
                break;
        }
        if (a == 3) {} //@ALWAYS_TRUE

        x = 3;
        a = 0;
        switch (x)
        {
            case 0: //@ALWAYS_FALSE
                a += 1;
            case 1: //@ALWAYS_FALSE
                a += 1;
            case 3: //@ALWAYS_TRUE
                a += 2;
            default:
                break;
        }
        if (a == 2) {} //@ALWAYS_TRUE
    }

    void testCase1()
    {
        int a = 5;
        switch(a)
        {
            case 1: //@ALWAYS_FALSE
                break;
            case 3: //@ALWAYS_FALSE
                break;
            case 5: //@ALWAYS_TRUE
                break;
        }
    }

    void testCase2(int a)
    {
        if (a == 3 || a == 4 || a == 5)
        {
            switch(a)
            {
                case 1: //@ALWAYS_FALSE
                    break;
                case 3:
                    break;
                case 5:
                    break;
            }
        }
    }

    void testCase3(int a)
    {
        if (a == 3 || a == 5)
        {
            switch(a)
            {
                case 1: //@ALWAYS_FALSE
                    break;
                case 3:
                    break;
                case 5:
                    break;
            }
        }
    }

    void testCase4(int a)
    {
        if (a != 3 && a != 5)
        {
            switch (a)
            {
                case 1:
                    break;
                case 3: //@ALWAYS_FALSE
                    break;
                case 5: //@ALWAYS_FALSE
                    break;
            }
        }
    }

    void testSwitchUnboxing1()
    {
        Integer x = new Integer(42);
        int z = 5;
        switch(x)
        {
            case 42: //@ALWAYS_TRUE
                z = 142;
                break;
            default:
                break;
        }
        if (z == 142) {} //@ALWAYS_TRUE
    }

    void testSwitchUnboxing2()
    {
        byte x = 42;
        int z = 5;
        switch(x)
        {
            case 42: //@ALWAYS_TRUE
                z = 142;
                break;
            default:
                break;
        }
        if (z == 142) {} //@ALWAYS_TRUE
    }

    void testSwitchUnboxing3()
    {
        String x = "text";
        int z = 5;
        switch(x)
        {
            case "text":
                z = 142;
                break;
            case "other":
                z = 5;
                break;
            default:
                break;
        }
        if (z == 142) {} //TODO: strings
        if (z == 5) {} //TODO: strings
        if (z == 6) {} //@ALWAYS_FALSE
    }

    void testSwitchUnboxing4()
    {
        SomeEnum x = SomeEnum.One;
        int z = 5;
        switch(x)
        {
            case One:
                z = 142;
                break;
            case Two:
                z = 5;
                break;
            default:
                break;
        }
        if (z == 142) {} //TODO: enums
        if (z == 5) {} //TODO: enums
        if (z == 6) {} //@ALWAYS_FALSE
    }

    void testDefaultOrder1(int a)
    {
        switch (a)
        {
            case 1:
                System.out.println("one");
                break;
            default:
                System.out.println("default");
                break;
            case 2: //ok
                System.out.println("two");
                break;
            case 3: //ok
                System.out.println("three");
                break;
        }
    }

    void testDefaultOrder2()
    {
        int x = 0;
        int a = 0;
        switch (x)
        {
            case 0: //@ALWAYS_TRUE
                a += 1;
            default:
                a += 1000;
            case 1: //@ALWAYS_FALSE
                a += 2;
            case 2: //@ALWAYS_FALSE
                a += 3;
        }
        if (a == 1006) {} //@ALWAYS_TRUE
        if (a == 6) {} //@ALWAYS_FALSE
    }

    void testDefaultOrder3()
    {
        int x = 1000;
        int a = 0;
        switch (x)
        {
            case 0: //@ALWAYS_FALSE
                a += 1;
            default:
                a += 1000;
            case 1: //@ALWAYS_FALSE
                a += 2;
            case 2: //@ALWAYS_FALSE
                a += 3;
        }
        if (a == 1005) {} //@ALWAYS_TRUE
    }

    void testDefaultOrder4()
    {
        int x = 1000;
        int a = 0;
        switch (x)
        {
            case 0: //@ALWAYS_FALSE
                a += 1;
            default:
                a += 1000;
                break;
            case 1: //@ALWAYS_FALSE
                a += 2;
            case 2: //@ALWAYS_FALSE
                a += 3;
        }
        if (a == 1000) {} //@ALWAYS_TRUE
    }

    void testDefaultOrder5()
    {
        int x = 0;
        int a = 0;
        switch (x)
        {
            case 0: //@ALWAYS_TRUE
                a += 1;
            default:
                a += 1000;
                break;
            case 1: //@ALWAYS_FALSE
                a += 2;
            case 2: //@ALWAYS_FALSE
                a += 3;
        }
        if (a == 1001) {} //@ALWAYS_TRUE
    }

    void testDefaultOrder6()
    {
        int x = 0;
        int a = 0;
        switch (x)
        {
            default:
                a = 1;
            case 1: //@ALWAYS_FALSE
                a += 2;
            case 2: //@ALWAYS_FALSE
                a += 3;
                break;
            case 3: //@ALWAYS_FALSE
                a += 5;
        }
        if (a == 6) {} //@ALWAYS_TRUE
    }
}
