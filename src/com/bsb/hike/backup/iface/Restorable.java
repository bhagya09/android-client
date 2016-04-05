package com.bsb.hike.backup.iface;

/**
 * Created by atul on 05/04/16.
 */
public interface Restorable
{
    boolean preRestoreSetup() throws Exception;

    void restore() throws Exception;

    void postRestoreSetup() throws Exception;

    void finish();
}
