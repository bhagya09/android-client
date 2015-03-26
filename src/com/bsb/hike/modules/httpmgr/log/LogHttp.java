package com.bsb.hike.modules.httpmgr.log;

import com.bsb.hike.modules.httpmgr.log.HttpLogger.DebugTree;
import com.bsb.hike.modules.httpmgr.log.HttpLogger.TREE_OF_SOULS;

public class LogHttp extends DebugTree
{
	private static long id = System.currentTimeMillis() + 5;
	
	public LogHttp(String tag)
	{
		super(id, tag);
	}


	/** Log a verbose message with optional format args. */
	public static void v(String message, Object... args)
	{
		TREE_OF_SOULS.verbose(id, message, args);
	}

	/** Log a verbose exception and a message with optional format args. */
	public static void v(Throwable t, String message, Object... args)
	{
		TREE_OF_SOULS.verbose(id, t, message, args);
	}

	/** Log a debug message with optional format args. */
	public static void d(String message, Object... args)
	{
		TREE_OF_SOULS.debug(id, message, args);
	}

	/** Log a debug exception and a message with optional format args. */
	public static void d(Throwable t, String message, Object... args)
	{
		TREE_OF_SOULS.debug(id, t, message, args);
	}

	/** Log an info message with optional format args. */
	public static void i(String message, Object... args)
	{
		TREE_OF_SOULS.info(id, message, args);
	}

	/** Log an info exception and a message with optional format args. */
	public static void i(Throwable t, String message, Object... args)
	{
		TREE_OF_SOULS.info(id, t, message, args);
	}

	/** Log a warning message with optional format args. */
	public static void w(String message, Object... args)
	{
		TREE_OF_SOULS.warn(id, message, args);
	}

	/** Log a warning exception and a message with optional format args. */
	public static void w(Throwable t, String message, Object... args)
	{
		TREE_OF_SOULS.warn(id, t, message, args);
	}

	/** Log an error message with optional format args. */
	public static void e(String message, Object... args)
	{
		TREE_OF_SOULS.error(id, message, args);
	}

	/** Log an error exception and a message with optional format args. */
	public static void e(Throwable t, String message, Object... args)
	{
		TREE_OF_SOULS.error(id, t, message, args);
	}
}
