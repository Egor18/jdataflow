package com.github.egor18.jdataflow.exceptions;

import spoon.reflect.cu.SourcePosition;

public class JDataFlowException extends RuntimeException
{
    private SourcePosition position;

    public JDataFlowException(SourcePosition position)
    {
        super();
        this.position = position;
    }

    public JDataFlowException(Exception e, SourcePosition position)
    {
        super(e);
        this.position = position;
    }

    public SourcePosition getPosition()
    {
        return position;
    }

    public void setPosition(SourcePosition position)
    {
        this.position = position;
    }
}
