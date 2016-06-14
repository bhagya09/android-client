package com.bsb.hike.spaceManager.models;

import com.bsb.hike.utils.CustomAnnotation.DoNotObfuscateSubClasses;

/**
 * @author paramshah
 */
@DoNotObfuscateSubClasses
public abstract class SpaceManagerPojo
{
    private String className;
    private String header;

    public String getClassName()
    {
        return className;
    }

    public String getHeader()
    {
        return header;
    }

    public void setClassName(String className)
    {
        this.className = className;
    }

    public void setHeader(String header)
    {
        this.header = header;
    }

    public abstract String toString();

}
