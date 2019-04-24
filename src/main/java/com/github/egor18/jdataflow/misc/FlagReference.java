package com.github.egor18.jdataflow.misc;

import spoon.support.reflect.reference.CtVariableReferenceImpl;

/**
 * Artificial CtVariableReference to represent flags like continueFlagReference, breakFlagReference and so on.
 */
public class FlagReference extends CtVariableReferenceImpl<Boolean>
{
    public FlagReference(String flagName)
    {
        setSimpleName(flagName);
        setType(getFactory().Type().BOOLEAN);
    }
}
