package com.github.egor18.jdataflow;

import spoon.reflect.cu.SourcePosition;
import spoon.reflect.cu.position.NoSourcePosition;
import spoon.reflect.declaration.CtElement;

import java.io.File;
import java.io.IOException;

public class Configuration
{
    private String[] sources;
    private String classpath;
    private String classpathFile;
    private String outputFile;
    private String[] excludes;
    private String[] includes;
    private String relativizer;
    private boolean noFailsafe;

    public String[] getSources()
    {
        return sources;
    }

    public void setSources(String[] sources)
    {
        this.sources = sources;
    }

    public String getClasspath()
    {
        return classpath;
    }

    public void setClasspath(String classpath)
    {
        this.classpath = classpath;
    }

    public String getClasspathFile()
    {
        return classpathFile;
    }

    public void setClasspathFile(String classpathFile)
    {
        this.classpathFile = classpathFile;
    }

    public String getOutputFile()
    {
        return outputFile;
    }

    public void setOutputFile(String outputFile)
    {
        this.outputFile = outputFile;
    }

    public String[] getExcludes()
    {
        return excludes;
    }

    public void setExcludes(String[] excludes)
    {
        this.excludes = excludes;
    }

    public String[] getIncludes()
    {
        return includes;
    }

    public void setIncludes(String[] includes)
    {
        this.includes = includes;
    }

    public String getRelativizer()
    {
        return relativizer;
    }

    public void setRelativizer(String relativizer)
    {
        this.relativizer = relativizer;
    }

    public boolean isNoFailsafe()
    {
        return noFailsafe;
    }

    public void setNoFailsafe(boolean noFailsafe)
    {
        this.noFailsafe = noFailsafe;
    }

    private boolean isSubElement(File file, String[] elements)
    {
        if (file == null || elements == null)
        {
            return false;
        }

        try
        {
            String filePath = file.getCanonicalPath();
            for (String element : elements)
            {
                String elementPath = new File(element).getCanonicalPath();
                if (filePath.startsWith(elementPath))
                {
                    return true;
                }
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        return false;
    }

    public boolean isInExcludedFile(CtElement element)
    {
        SourcePosition position = element.getPosition();
        if (position == null || position instanceof NoSourcePosition || position.getFile() == null)
        {
            return false;
        }

        return isSubElement(position.getFile(), excludes) && !isSubElement(position.getFile(), includes);
    }
}
