package com.github.egor18.jdataflow;

import com.google.gson.annotations.SerializedName;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.cu.position.NoSourcePosition;
import spoon.reflect.declaration.CtElement;

import java.io.File;
import java.io.IOException;

public class Configuration
{
    @SerializedName("sources")
    private String[] sources;

    @SerializedName("classpath")
    private String classpath;

    @SerializedName("classpath-file")
    private String classpathFile;

    @SerializedName("output")
    private String output;

    @SerializedName("excludes")
    private String[] excludes;

    @SerializedName("includes")
    private String[] includes;

    @SerializedName("relativizer")
    private String relativizer;

    @SerializedName("no-failsafe")
    private boolean noFailsafe;

    private transient String configFile;

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

    public String getOutput()
    {
        return output;
    }

    public void setOutput(String output)
    {
        this.output = output;
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

    public String getConfigFile()
    {
        return configFile;
    }

    public void setConfigFile(String configFile)
    {
        this.configFile = configFile;
    }
}
