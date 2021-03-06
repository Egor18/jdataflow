package com.github.egor18.jdataflow.testcases;

import java.util.List;

public class TestArray
{
    void testArray1()
    {
        int[] arr = new int[3];
        if (arr == null) {} //@ALWAYS_FALSE
        arr[0] = 1;
        arr[1] = 2;
        arr[2] = 3;
        if (arr[0] == 1) {} //@ALWAYS_TRUE
        if (arr[1] == 2) {} //@ALWAYS_TRUE
        if (arr[2] == 3) {} //@ALWAYS_TRUE
    }

    void testArray2(int x)
    {
        int[] arr = new int[3];
        int idx = 0;
        arr[idx] = 1;
        arr[1] = x;
        if (arr[0] == 1) {} //@ALWAYS_TRUE
        if (arr[0 + 3 - 2 + 0] == x) {} //@ALWAYS_TRUE
    }

    void testArray3(int[] arr)
    {
        arr[4] = 42;
        if (arr[1 + 1 + 1 + 1] == 42) {} //@ALWAYS_TRUE
    }

    void testArray4(boolean cond)
    {
        int[] arr = new int[2];
        int[] other = null;
        if (arr == null) {} //@ALWAYS_FALSE
        if (arr[0] == 0) {}
        if (cond)
        {
            arr = other;
        }
        if (arr == other) {} //ok
        if (arr == null) {} //ok
    }

    void testArray5()
    {
        int arr[] = new int[2];
        arr[0] = 1;
        arr[1] = 2;
        int other[] = new int[2];
        other[0] = 10;
        other[1] = 20;
        arr = other;
        arr[1] = 142;
        if (arr[0] == 10) {} //@ALWAYS_TRUE
        if (arr[1] == 142) {} //@ALWAYS_TRUE
        if (other[0] == 10) {} //@ALWAYS_TRUE
        if (other[1] == 142) {} //@ALWAYS_TRUE
    }

    void testArray6(boolean cond)
    {
        int arr[] = new int[2];
        arr[0] = 0;
        arr[1] = 0;
        if (cond)
        {
            arr[1] = 5;
        }
        arr[0] = 6;
        if (cond)
        {
            if (arr[0] == 6) {} //@ALWAYS_TRUE
            if (arr[1] == 5) {} //@ALWAYS_TRUE
        }
        if (arr[0] == 6) {} //@ALWAYS_TRUE
        if (arr[1] == 5) {} //ok
    }

    void testArray7()
    {
        char arr1[] = new char[]{'f', 'z', 'x'};
        arr1[0] = 'a';
        if (arr1[0] == 'a') {} //@ALWAYS_TRUE

        Object arr2[] = new Object[3];
        arr2[0] = new Object();
        arr2[1] = null;
        if (arr2[0] == null) {} //@ALWAYS_FALSE
        if (arr2[1] == null) {} //@ALWAYS_TRUE
        if (arr2[2] == null) {}
    }

    void testArray8()
    {
        int mat[][] = new int[2][2];
        mat[0][0] = 1;
        if (mat[0][0] == 1) {} //@ALWAYS_TRUE
        if (mat[0] == null) {} //@ALWAYS_FALSE
        mat[1] = null;
        if (mat[1] == null) {} //@ALWAYS_TRUE
    }

    void testArray9()
    {
        int[][] mat =
        {
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9}
        };
        if (mat == null) {} //@ALWAYS_FALSE
        if (mat[0] == null) {} //@ALWAYS_FALSE
        if (mat[0][0] == 1) {} //@ALWAYS_TRUE
        if (mat[2][1] == 8) {} //@ALWAYS_TRUE
    }

    void testArray10()
    {
        int[][][] cube =
        {
            {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}},
            {{10, 11, 12}, {13, 14, 15}, {16, 17, 18}},
            {{19, 20, 21}, {22, 23, 24}, {25, 26, 27}}
        };
        if (cube[0][0][0] == 1) {} //@ALWAYS_TRUE
        if (cube[1][2][2] == 18) {} //@ALWAYS_TRUE
        if (cube[1][1][1] == 14) {} //@ALWAYS_TRUE
        if (cube[2][1][0] == 22) {} //@ALWAYS_TRUE
        if (cube[0][1][2] == 6) {} //@ALWAYS_TRUE

        int[][][] cube2 = cube;
        if (cube2[1][2][2] == 18) {} //@ALWAYS_TRUE
        if (cube2[1] == cube[1]) {} //@ALWAYS_TRUE
        if (cube2[1] == cube2[1]) {} //@ALWAYS_TRUE
    }

    void testArray11()
    {
        int[] arr = {0, 0, 0, 0};
        byte i = 3;
        arr[i] = 4;
        arr[(int)3L] = 4;
        if (arr[i] == 4) {} //@ALWAYS_TRUE
        if (arr[(int) 3L] == 4) {} //@ALWAYS_TRUE
        arr[i] = i;
        if (arr[i] == i) {} //@ALWAYS_TRUE
    }

    void testArray12()
    {
        int i = 0;
        int[] arr = {1, 2, 3, 4, 5};
        if (arr[i++] == 1) {} //@ALWAYS_TRUE
        if (arr[i] == 2) {} //@ALWAYS_TRUE
        if (i == 1) {} //@ALWAYS_TRUE
        if (++arr[i] == 3) {} //@ALWAYS_TRUE
    }

    void testArray13()
    {
        int i = 2;
        int[] arr = new int[++i];
        if (i == 3) {} //@ALWAYS_TRUE
    }

    void testArray14()
    {
        int i = 0;
        int[] arr = {1, 2, 3, 4, 5};
        arr[i += 1]++;
        if (arr[0] == 1) {} //@ALWAYS_TRUE
        if (i == 0) {} //@ALWAYS_FALSE
        if (i == 1) {} //@ALWAYS_TRUE
    }

    void testArray15()
    {
        int i = 0;
        int[] arr = {1, 2, 3, 4, 5};
        arr[i++]++;
        if (arr[0] == 2) {} //@ALWAYS_TRUE
        if (i == 0) {} //@ALWAYS_FALSE
        if (i == 1) {} //@ALWAYS_TRUE
    }

    void testArray16()
    {
        int i = 0;
        int[] arr = {1, 2, 3, 4, 5};
        arr[++i]++;
        if (arr[0] == 1) {} //@ALWAYS_TRUE
        if (arr[1] == 3) {} //@ALWAYS_TRUE
        if (i == 0) {} //@ALWAYS_FALSE
        if (i == 1) {} //@ALWAYS_TRUE
    }

    void testArray17()
    {
        int[] arr = {1, 2, 3, 4, 5};
        if (arr['b' - 'a'] == 2) {} //@ALWAYS_TRUE
        byte b = 3;
        if (arr[b] == 4) {} //@ALWAYS_TRUE
    }

    void testArray18(int[] arr)
    {
        if (arr['a'] == 42) {} //ok
        byte b = 3;
        if (arr[b] == 42) {} //ok
    }

    void testArray19()
    {
        boolean[] arr = {false, true, false};
        if (arr[1] && true) {} //@ALWAYS_TRUE
    }

    void testArray20()
    {
        if (new int[] {1,2,3} == null) {} //@ALWAYS_FALSE
    }

    void testArray21()
    {
        int[] arr = {1,2,3};
        if (arr[new Integer(2)] == 3) {} //@ALWAYS_TRUE
    }

    void testArrayLength1()
    {
        char[] arr1 = new char[3];
        if (arr1.length == 3) {} //@ALWAYS_TRUE

        char[] arr2 = new char[]{'1', '2', '3', '4'};
        if (arr2.length == 4) {} //@ALWAYS_TRUE

        int[][] arr3 = new int[6][8];
        if (arr3.length == 6) {} //@ALWAYS_TRUE
        if (arr3[0].length == 8) {}
    }

    void testArrayLength2(int[] arr1)
    {
        if (arr1.length == 10) {} //ok
        if (arr1.length == 0) {} //ok
        if (arr1.length == -1) {} //@ALWAYS_FALSE
        if (arr1.length < 0) {} //@ALWAYS_FALSE
        if (arr1.length == -10) {} //@ALWAYS_FALSE
    }

    void testArrayLength3(boolean cond)
    {
        int[] arr;
        if (cond)
        {
            arr = new int[5];
        }
        else
        {
            arr = new int[10];
        }
        if (arr.length == 5) {} //ok
        if (arr.length == 10) {} //ok
        if (arr.length == 4) {} //@ALWAYS_FALSE
        if (arr.length != 5 && arr.length != 10) {} //@ALWAYS_FALSE
    }

    void testArrayLength4()
    {
        int[] arr1 = new int[4];
        int[] arr2 = new int[4];
        int[] arr3 = new int[6];
        if (arr1.length == arr3.length) {} //@ALWAYS_FALSE
        if (arr1.length == arr2.length) {} //@ALWAYS_TRUE
    }

    void testArrayLength5(boolean cond)
    {
        int len = cond ? 5 : 7;
        int[] arr1 = new int[len];
        if (cond)
        {
            if (arr1.length == 5) {} //@ALWAYS_TRUE
        }
        else
        {
            if (arr1.length == 7) {} //@ALWAYS_TRUE
        }
    }

    void testArrayLength6()
    {
        Integer len = new Integer(5);
        int[] arr1 = new int[len];
        if (arr1.length == 5) {} //@ALWAYS_TRUE
    }

    public Integer integerField;
    void testArrayLength7()
    {
        integerField = new Integer(5);
        int[] arr1 = new int[integerField];
        if (arr1.length == 5) {} //@ALWAYS_TRUE
    }

    void testArrayLength8()
    {
        byte len = 10;
        int[] arr = new int[len];
        if (arr.length == 10) {} //@ALWAYS_TRUE
    }

    void testArrayLength9(Object size)
    {
        Object[] arr1 = new Object[(Integer) size];
        if (arr1.length == (Integer) size) {} //@ALWAYS_TRUE
    }

    public int[] publicArrayField;
    void testArrayField1()
    {
        publicArrayField[3] = 3;
        if (publicArrayField[3] == 3) {} //@ALWAYS_TRUE

        if (publicArrayField[3] + 1 > 2) {} //@ALWAYS_TRUE

        if (publicArrayField[4] == 5)
        {
            if (publicArrayField[2 + 2] == 5) {} //@ALWAYS_TRUE
        }

        publicArrayField[3]++;
        if (publicArrayField[3] == 4) {} //@ALWAYS_TRUE
    }

    void testArrayFieldLength1()
    {
        if (publicArrayField.length == 10) {} //ok
        if (publicArrayField.length >= 0) {} //@ALWAYS_TRUE
        if (publicArrayField.length == -1) {} //@ALWAYS_FALSE
    }

    private class DateTime
    {
        int year, month, day, hour, minute, second;
    }
    static boolean isLeapYear(int year)
    {
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
    }
    boolean testOneFamousBug(DateTime time)
    {
        int[] kDaysInMonth = { 0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
        if (time.year < 1 || time.year > 9999 ||
                time.month < 1 || time.month > 12 ||
                time.day < 1 || time.day > 31 ||
                time.hour < 0 || time.hour > 23 ||
                time.minute < 0 || time.minute > 59 ||
                time.second < 0 || time.second > 59)
        {
            return false;
        }
        if (time.month == 2 && isLeapYear(time.year))
        {
            return time.month <= kDaysInMonth[time.month] + 1; //@ALWAYS_TRUE
        }
        else
        {
            return time.month <= kDaysInMonth[time.month]; //@ALWAYS_TRUE
        }
    }

    void testArrayElementsCast1()
    {
        long arr1[] = {1, 2, 3L, 4L};
        if (arr1[0] == 1) {} //@ALWAYS_TRUE
        if (arr1[1] == 2) {} //@ALWAYS_TRUE
        if (arr1[2] == 3) {} //@ALWAYS_TRUE
        if (arr1[3] == 4) {} //@ALWAYS_TRUE

        long arr2[] = {(byte)1000, (int)2, (char)3, 4};
        if (arr2[0] == 1000) {} //@ALWAYS_FALSE
        if (arr2[1] == 2) {} //@ALWAYS_TRUE
        if (arr2[2] == 3) {} //@ALWAYS_TRUE
        if (arr2[3] == 4) {} //@ALWAYS_TRUE
    }

    void testArrayParam1(int[] arr)
    {
        if (arr == null) {} //ok
        arr[0] = 5;
        if (arr[0] == 5) {} //@ALWAYS_TRUE
        if (arr[1] == 5) {} //ok
    }

    void testArrayOperations1()
    {
        int[] arr1 = {1, 2, 3};
        arr1[0]++;
        arr1[0]++;
        arr1[0]++;
        if (arr1[0] == 4) {} //@ALWAYS_TRUE
    }

    void testArrayOperations2()
    {
        int[] arr1 = {1, 2, 3};
        int[] arr2 = {4, 5, 6};
        if (arr1[0] + arr2[1] == 6) {} //@ALWAYS_TRUE
    }

    void testArrayOperations3()
    {
        int[] arr1 = {1, 2, 3};
        arr1[0] += 1;
        if (arr1[0] == 2) {} //@ALWAYS_TRUE
    }

    void testArrayOperations4()
    {
        int[] arr1 = {1, 2, 3};
        if (arr1[0]++ == 1) {}  //@ALWAYS_TRUE
        if (arr1[0] == 2) {} //@ALWAYS_TRUE
        if (++arr1[0] == 3) {} //@ALWAYS_TRUE
        if (arr1[0] == 3) {} //@ALWAYS_TRUE
    }

    void testArrayIndexUnboxing1()
    {
        int[] arr = {1, 2, 3};
        Integer i = 1;
        arr[i] = 5;
        if (arr[i] == 5) {} //@ALWAYS_TRUE
    }

    void testArrayIndirectReset1(List<Integer> elements)
    {
        int[] arr = {1, 2, 3};
        elements.forEach(e -> arr[e] = e);
        if (arr[0] == 1) {} //ok
        if (arr[1] == 2) {} //ok
        if (arr[2] == 3) {} //ok
    }

    void testArrayIndirectReset2(List<Integer> elements)
    {
        long[] arr = {1, 2, 3};
        elements.forEach(e -> arr[e] = e);
        if (arr[0] == 1) {} //ok
        if (arr[1] == 2) {} //ok
        if (arr[2] == 3) {} //ok
    }

    void testArrayIndirectReset3(List<Integer> elements)
    {
        float[] arr = {1, 2, 3};
        elements.forEach(e -> arr[e] = e);
        if (arr[0] == 1) {} //ok
        if (arr[1] == 2) {} //ok
        if (arr[2] == 3) {} //ok
    }

    void testArrayInitializers1()
    {
        int[] arr1 = {1, 2, 3};
        if (arr1[1] == 2) {} //@ALWAYS_TRUE
        long[] arr2 = {1, 2, 3};
        if (arr2[1] == 2) {} //@ALWAYS_TRUE
        float[] arr3 = {1, 2, 3};
        if (arr3[1] == 2) {}
        double[] arr4 = {1, 2, 3};
        if (arr4[1] == 2) {}

        int[] arr5 = {1, 'c', 2};
        if (arr5[0] == 1) {} //@ALWAYS_TRUE
        if (arr5[1] == 'c') {} //@ALWAYS_TRUE

        Integer[] arr6 = {1, new Integer(2), 3};
        if (arr6[0] == 1) {} //@ALWAYS_TRUE
        if (arr6[1] == 2) {} //@ALWAYS_TRUE
        if (arr6[2] == 3) {} //@ALWAYS_TRUE

        int[] arr7 = {1, new Integer(2), 3};
        if (arr7[0] == 1) {} //@ALWAYS_TRUE
        if (arr7[1] == 2) {} //@ALWAYS_TRUE
        if (arr7[2] == 3) {} //@ALWAYS_TRUE

        float[] arr8 = {1.0f, 2.0f, 3.0f};
        if (arr8[0] == 2.0f) {}
        float[] arr9 = {1.0f, new Float(2.0f), 3.0f};
        if (arr9[0] == 2.0f) {}
        Float[] arr10 = {new Float(1),new Float(2),new Float(3)};
        if (arr10[0] == 2.0f) {}
        Float[] arr11 = {1.0f, 2.0f, 3.0f};
        if (arr11[0] == 2.0f) {}
    }

    class C
    {
        public int x;
        public native int f();
    }

    void testArrayIndexReset(C[] objects)
    {
        for (int i = 0; i < objects.length; i++)
        {
            int a = objects[42].f();
            int b = objects[i].f();
        }
    }

    void testArrayIndexUnboxing1(Integer i, Object[][] values)
    {
        values[i][i] = null;
        if (values[i][i] == null) {} //@ALWAYS_TRUE
    }

    void testArrayIndexUnboxing2()
    {
        int[] arr = new int[10];
        Integer idx = 1;
        arr[idx] = 42;
        arr[idx]++;
        if (arr[idx] == 43) {} //@ALWAYS_TRUE
    }

    native Integer getIndex();

    void testArrayIndexUnboxing3()
    {
        double[] arr = new double[10];
        arr[getIndex()] += 1;
    }

    void testArrayIndexOutOfBounds1()
    {
        byte[] arr = new byte[] {1, 2, 4, 8, 16, 32};
        byte a1 = arr[9]; //@ARRAY_INDEX_IS_OUT_OF_BOUNDS
        byte a2 = arr[6]; //@ARRAY_INDEX_IS_OUT_OF_BOUNDS
        byte a3 = arr[(int)5L]; //ok
        byte a4 = arr[0]; //ok
        byte a5 = arr[-1]; //@ARRAY_INDEX_IS_OUT_OF_BOUNDS
        byte a6 = arr[-5]; //@ARRAY_INDEX_IS_OUT_OF_BOUNDS
    }

    void testArrayIndexOutOfBounds2()
    {
        byte[] arr = new byte[] {1, 2, 4, 8, 16, 32};
        arr[9] = 42; //@ARRAY_INDEX_IS_OUT_OF_BOUNDS
        arr[6] = 42; //@ARRAY_INDEX_IS_OUT_OF_BOUNDS
        arr[(int)5L] = 42; //ok
        arr[0] = 42; //ok
        arr[-1] = 42; //@ARRAY_INDEX_IS_OUT_OF_BOUNDS
        arr[-5] = 42; //@ARRAY_INDEX_IS_OUT_OF_BOUNDS
    }

    void testArrayIndexOutOfBounds3()
    {
        byte[] arr = new byte[] {1, 2, 4, 8, 16, 32};
        Integer i1 = new Integer(3);
        Integer i2 = new Integer(10);
        arr[i1] = 42; //ok
        arr[i2] = 42; //@ARRAY_INDEX_IS_OUT_OF_BOUNDS
    }

    void testArrayIndexOutOfBounds4(int[] arr)
    {
        if (arr.length < 10)
        {
            int a1 = arr[10]; //@ARRAY_INDEX_IS_OUT_OF_BOUNDS
        }
    }

    void testArrayIndexOutOfBounds5(int[] arr, int i)
    {
        int a1 = arr[i]; //ok
        arr[-1] = 42; //@ARRAY_INDEX_IS_OUT_OF_BOUNDS
    }

    void testArrayIndexOutOfBounds6()
    {
        int[] arr = {0, 0, 0, 0};
        byte i = 8;
        arr[i] = 4; //@ARRAY_INDEX_IS_OUT_OF_BOUNDS
    }

    void testArrayIndexPromotion1()
    {
        byte i = 0;
        int[] arr = {1, 2, 3};
        arr[i]++;
        if (arr[i] == 2) {} //@ALWAYS_TRUE
    }

    void testArrayIndexPromotion2()
    {
        byte i = 0;
        int[] arr = {1, 2, 3};
        arr[i] = 0;
        if (arr[i] == 0) {} //@ALWAYS_TRUE
    }

    void testArrayIndexPromotion3()
    {
        byte i = 0;
        int[] arr = {1, 2, 3};
        arr[i] += 1;
        if (arr[i] == 2) {} //@ALWAYS_TRUE
    }

    class A { public int[] arr; }

    public A[] marr;

    void testArrayIndexPromotion4()
    {
        byte i = 0;
        marr[i].arr[i] = 0;
        if (marr[i].arr[i] == 0) {} //@ALWAYS_TRUE
    }
}
