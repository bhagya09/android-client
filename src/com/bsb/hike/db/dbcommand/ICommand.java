package com.bsb.hike.db.dbcommand;

/**
 * Created by sidharth on 22/03/16.
 */
public interface ICommand<T>
{
    T execute();
}
