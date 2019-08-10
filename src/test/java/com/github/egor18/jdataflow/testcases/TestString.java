package com.github.egor18.jdataflow.testcases;

public class TestString
{
    void testStringConcatenation1()
    {
        String s0 = "one";
        String s1 = "one" + "two";
        String s2 = "one" + "two" + "three";
        String s3 = "one" + "two" + "three" + "four";
        String s4 = "one" + "two" + "three" + "four" + "five" + "six" + "seven" + "eight";
        String s5 = s1 + s2 + s3 + s4;
        if (s0 == null) {} //@ALWAYS_FALSE
        if (s1 == null) {} //@ALWAYS_FALSE
        if (s2 == null) {} //@ALWAYS_FALSE
        if (s3 == null) {} //@ALWAYS_FALSE
        if (s4 == null) {} //@ALWAYS_FALSE
        if (s5 == null) {} //@ALWAYS_FALSE
    }

    void testStringConcatenation2(String s1, String s2)
    {
        if (s1 == null) {} //ok
        if (s2 == null) {} //ok
        if ((s1 + s2) == null) {} //@ALWAYS_FALSE
        if ((s1 + "one") == null) {} //@ALWAYS_FALSE
    }
}
