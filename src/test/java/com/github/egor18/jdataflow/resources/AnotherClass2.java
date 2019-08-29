package com.github.egor18.jdataflow.resources;

public class AnotherClass2
{
    public static final int X = 10;
    public static final int Y = AnotherClass1.Y;
    public static final int recursiveField1 = AnotherClass1.recursiveField1;
    public static final int recursiveField2 = AnotherClass1.recursiveField2;
    public static final int recursiveField3 = recursiveField2;
}
