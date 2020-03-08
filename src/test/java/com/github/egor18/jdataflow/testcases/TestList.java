package com.github.egor18.jdataflow.testcases;

import java.util.*;

public class TestList
{
    void testListConstructor1()
    {
        List<Integer> list1 = new ArrayList<>();
        if (list1.isEmpty()) {} //@ALWAYS_TRUE
        if (list1.size() == 0) {} //@ALWAYS_TRUE

        List<Integer> list2 = new LinkedList<>();
        if (list2.isEmpty()) {} //@ALWAYS_TRUE
        if (list2.size() == 0) {} //@ALWAYS_TRUE

        List<Integer> list3 = new Stack<>();
        if (list3.isEmpty()) {} //@ALWAYS_TRUE
        if (list3.size() == 0) {} //@ALWAYS_TRUE

        List<Integer> list4 = new Vector<>();
        if (list4.isEmpty()) {} //@ALWAYS_TRUE
        if (list4.size() == 0) {} //@ALWAYS_TRUE
    }

    void testListAdd1()
    {
        List<Integer> list = new ArrayList<>();
        list.add(10);
        if (list.add(20)) {} //@ALWAYS_TRUE
        list.add(new Integer(30));
        if (list.size() == 3) {} //@ALWAYS_TRUE
        if (list.get(0) == 10) {} //@ALWAYS_TRUE
        if (list.get(1) == 20) {} //@ALWAYS_TRUE
        if (list.get(2) == 30) {} //@ALWAYS_TRUE
        if (list.get(0) == null) {} //@ALWAYS_FALSE
        if (list.get(1) == null) {} //@ALWAYS_FALSE
        if (list.get(2) == null) {} //@ALWAYS_FALSE
    }

    void testListAdd2(List<Integer> list)
    {
        if (list.isEmpty()) {} //ok
        list.add(10);
        if (list.isEmpty()) {} //@ALWAYS_FALSE
        if (list.get(list.size() - 1) == 10) {} //@ALWAYS_TRUE
    }

    void testListSize1(List<Integer> list)
    {
        if (list.isEmpty()) {} //ok
        if (list.size() == 0) {} //ok
        if (list.size() <= 0) {} //ok
        if (list.size() < 0) {} //@ALWAYS_FALSE
    }

    void testListSet1()
    {
        List<Character> list = new ArrayList<>();
        list.add('f');
        if (list.get(0) == 'a') {} //@ALWAYS_FALSE
        if (list.set(0, 'a') == 'f') {} //@ALWAYS_TRUE
        if (list.get(0) == 'a') {}  //@ALWAYS_TRUE
        if (list.set(new Integer(0), 'z') == 'a') {} //@ALWAYS_TRUE
        if (list.get(0) == 'z') {} //@ALWAYS_TRUE
    }

    void testListGet1()
    {
        List<Character> list = new ArrayList<>();
        list.add('a');
        list.add('b');
        list.add('c');
        if (list.get(1) == 'b') {} //@ALWAYS_TRUE
        if (list.get(new Integer(1)) == 'b') {} //@ALWAYS_TRUE
    }

    void testListGet2(List<Character> list)
    {
        if (list.get(1) == 'a') {} //ok
        if (list.get(new Integer(1)) == 'a') {} //ok
    }

    public native void unknownFunc(List<Integer> list);

    void testListReset1()
    {
        List<Integer> list = new ArrayList<>();
        list.add(10);
        list.add(20);
        list.add(30);
        if (list.size() == 3) {} //@ALWAYS_TRUE
        if (list.get(1) == 20) {} //@ALWAYS_TRUE
        unknownFunc(list);
        if (list.size() == 3) {} //ok
        if (list.get(1) == 20) {} //ok
    }

    void testListReset2(int n)
    {
        List<Integer> list = new ArrayList<>();
        list.add(10);
        list.add(20);
        list.add(30);
        if (list.size() == 3) {} //@ALWAYS_TRUE
        if (list.get(1) == 20) {} //@ALWAYS_TRUE
        for (int i = 0; i < n; i++)
        {
            list.add(i);
        }
        if (list.size() == 3) {} //ok
        if (list.get(1) == 20) {} //ok
    }

    void testListContains1()
    {
        List<Integer> list = new ArrayList<>();
        if (list.contains(42)) {} //@ALWAYS_FALSE
        list.add(10);
        list.add(20);
        if (list.contains(42)) {} //ok
    }

    void testListContains2(List<Integer> list)
    {
        if (list.contains(42)) {} //ok
    }

    void testListNoGenericParameter1()
    {
        List list = new ArrayList();
        list.add(new Integer(10));
        list.add(new Integer(20));
        list.add(new Integer(30));
        if (list.size() == 3) {} //@ALWAYS_TRUE
        if (list.get(0) == null) {} //@ALWAYS_FALSE
        if (list.get(1) == null) {} //@ALWAYS_FALSE
        if (list.get(2) == null) {} //@ALWAYS_FALSE
        if ((Integer) list.get(0) == 10) {} //@ALWAYS_TRUE
        if ((Integer) list.get(1) == 20) {} //@ALWAYS_TRUE
        if ((Integer) list.get(2) == 30) {} //@ALWAYS_TRUE
    }

    static <T, U extends List<T>> T testGenericList1(U values)
    {
        return values.get(42); //ok
    }

    static <T extends List<T>, U extends T> T testGenericList2(U values)
    {
        return values.get(42); //ok
    }
}
