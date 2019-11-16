package com.github.egor18.jdataflow.exceptions;

import spoon.reflect.cu.SourcePosition;

public class JDataFlowTimeoutException extends JDataFlowException
{
    public JDataFlowTimeoutException(SourcePosition position)
    {
        super(position);
    }
}
