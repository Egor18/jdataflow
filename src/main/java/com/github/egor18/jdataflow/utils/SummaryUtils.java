package com.github.egor18.jdataflow.utils;

import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;

public class SummaryUtils
{
    public static String getFullSignature(String qualifiedTypeName, String signature)
    {
        return qualifiedTypeName + "." + signature;
    }

    public static String getFullSignature(CtTypeReference<?> declaringType, String signature)
    {
        return declaringType.getQualifiedName() + "." + signature;
    }

    public static String getFullSignature(CtTypeReference<?> declaringType, CtExecutableReference<?> executable)
    {
        return declaringType.getQualifiedName() + "." + executable.getSignature();
    }
}
