package com.github.egor18.jdataflow.resources;

public class AnotherClass1
{
    public static final int CONST1 = 42;
    public static int NON_CONST1 = 42;
    public static final int x = 10;
    public static final int y = 20;
    public static final int CONST2 = x + y;
    public static final int X = AnotherClass2.X;
    public static final int Y = 20;
    public static final int recursiveField1 = AnotherClass2.recursiveField1;
    public static final int recursiveField2 = AnotherClass2.recursiveField3;
    public static final int Z;
    public static final int W;
    public static final int P;
    public static boolean cond;
    static
    {
        Z = 42;
        W = Z > 0 ? 100 : 200;
        P = cond ? 100 : 200;
    }
}
