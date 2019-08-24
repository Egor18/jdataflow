package com.github.egor18.jdataflow.warning;

import spoon.reflect.cu.SourcePosition;
import spoon.reflect.cu.position.NoSourcePosition;
import spoon.reflect.declaration.CtElement;

import java.nio.file.Paths;

public class Warning
{
    public CtElement element;
    public WarningKind kind;
    public SourcePosition position;
    public String message;
    public String relativizer;

    public Warning(CtElement element, WarningKind kind)
    {
        this.element = element;
        this.kind = kind;
        this.position = element.getPosition();
        this.message = String.format(kind.message, element.toString());
        this.relativizer = null;
    }

    @Override
    public String toString()
    {
        String positionString;
        if (position == null || position instanceof NoSourcePosition || position.getFile() == null)
        {
            positionString = "(unknown file)";
        }
        else
        {
            String filename;
            try
            {
                filename = "./" + Paths.get(relativizer).relativize(Paths.get(position.getFile().toURI())).toString();
            }
            catch (Exception e)
            {
                filename = position.getFile().toString();
            }
            filename = filename.replace("\\", "/");
            positionString = String.format("(%s:%s)", filename, position.getLine());
        }
        return String.format("%s %s", message, positionString);
    }
}
