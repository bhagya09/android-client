package com.bsb.hike.spaceManager.models;

/**
 * @author paramshah
 */
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
