package com.bsb.hike.modules.httpmgr.log;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.util.Log;
import android.util.SparseBooleanArray;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bsb.hike.HikeMessengerApp;
import com.bsb.hike.utils.Logger;

/** Logging for lazy people. */
public final class HttpLogger
{
	public static boolean DEBUG = false;

	/** Add a new logging tree. */
	public static void plant(Tree tree)
	{
		if (tree == null)
		{
			throw new NullPointerException("tree == null");
		}
		if (tree instanceof TaggedTree)
		{
			TAGGED_TREES.append(FOREST.size(), true);
		}
		FOREST.add(tree);
	}

	/** Remove a planted tree. */
	public static void uproot(Tree tree)
	{
		for (int i = 0, size = FOREST.size(); i < size; i++)
		{
			if (FOREST.get(i) == tree)
			{
				TAGGED_TREES.delete(i);
				FOREST.remove(i);
				return;
			}
		}
		throw new IllegalArgumentException("Cannot uproot tree which is not planted: " + tree);
	}

	/** Remove all planted trees. */
	public static void uprootAll()
	{
		TAGGED_TREES.clear();
		FOREST.clear();
	}

	static final List<Tree> FOREST = new CopyOnWriteArrayList<Tree>();

	static final SparseBooleanArray TAGGED_TREES = new SparseBooleanArray();

	/** A {@link Tree} that delegates to all planted trees in the {@link #FOREST forest}. */
	public static final class TREE_OF_SOULS
	{
		public static void verbose(long id, String message, Object... args)
		{
			List<Tree> forest = FOREST;
			// noinspection ForLoopReplaceableByForEach
			for (int i = 0, count = forest.size(); i < count; i++)
			{
				Tree tree = forest.get(i);
				if (tree.getId() == id)
				{
					forest.get(i).verbose(message, args);
				}
			}
		}

		public static void verbose(long id, Throwable t, String message, Object... args)
		{
			List<Tree> forest = FOREST;
			// noinspection ForLoopReplaceableByForEach
			for (int i = 0, count = forest.size(); i < count; i++)
			{
				Tree tree = forest.get(i);
				if (tree.getId() == id)
				{
					forest.get(i).verbose(t, message, args);
				}
			}
		}

		public static void debug(long id, String message, Object... args)
		{
			List<Tree> forest = FOREST;
			// noinspection ForLoopReplaceableByForEach
			for (int i = 0, count = forest.size(); i < count; i++)
			{
				Tree tree = forest.get(i);
				if (tree.getId() == id)
				{
					forest.get(i).debug(message, args);
				}
			}
		}

		public static void debug(long id, Throwable t, String message, Object... args)
		{
			List<Tree> forest = FOREST;
			// noinspection ForLoopReplaceableByForEach
			for (int i = 0, count = forest.size(); i < count; i++)
			{
				Tree tree = forest.get(i);
				if (tree.getId() == id)
				{
					forest.get(i).debug(t, message, args);
				}
			}
		}

		public static void info(long id, String message, Object... args)
		{
			List<Tree> forest = FOREST;
			// noinspection ForLoopReplaceableByForEach
			for (int i = 0, count = forest.size(); i < count; i++)
			{
				Tree tree = forest.get(i);
				if (tree.getId() == id)
				{
					forest.get(i).info(message, args);
				}
			}
		}

		public static void info(long id, Throwable t, String message, Object... args)
		{
			List<Tree> forest = FOREST;
			// noinspection ForLoopReplaceableByForEach
			for (int i = 0, count = forest.size(); i < count; i++)
			{
				Tree tree = forest.get(i);
				if (tree.getId() == id)
				{
					forest.get(i).info(t, message, args);
				}
			}
		}

		public static void warn(long id, String message, Object... args)
		{
			List<Tree> forest = FOREST;
			// noinspection ForLoopReplaceableByForEach
			for (int i = 0, count = forest.size(); i < count; i++)
			{
				Tree tree = forest.get(i);
				if (tree.getId() == id)
				{
					forest.get(i).warn(message, args);
				}
			}
		}

		public static void warn(long id, Throwable t, String message, Object... args)
		{
			List<Tree> forest = FOREST;
			// noinspection ForLoopReplaceableByForEach
			for (int i = 0, count = forest.size(); i < count; i++)
			{
				Tree tree = forest.get(i);
				if (tree.getId() == id)
				{
					forest.get(i).warn(t, message, args);
				}
			}
		}

		public static void error(long id, String message, Object... args)
		{
			List<Tree> forest = FOREST;
			// noinspection ForLoopReplaceableByForEach
			for (int i = 0, count = forest.size(); i < count; i++)
			{
				forest.get(i).error(message, args);
			}
		}

		public static void error(long id, Throwable t, String message, Object... args)
		{
			List<Tree> forest = FOREST;
			// noinspection ForLoopReplaceableByForEach
			for (int i = 0, count = forest.size(); i < count; i++)
			{
				Tree tree = forest.get(i);
				if (tree.getId() == id)
				{
					forest.get(i).error(t, message, args);
				}
			}
		}
	}

	private HttpLogger()
	{
		throw new AssertionError("No instances.");
	}

	/** A facade for handling logging calls. Install instances via {@link #plant}. */
	public interface Tree
	{
		long getId();

		/** Log a verbose message with optional format args. */
		void verbose(String message, Object... args);

		/** Log a verbose exception and a message with optional format args. */
		void verbose(Throwable t, String message, Object... args);

		/** Log a debug message with optional format args. */
		void debug(String message, Object... args);

		/** Log a debug exception and a message with optional format args. */
		void debug(Throwable t, String message, Object... args);

		/** Log an info message with optional format args. */
		void info(String message, Object... args);

		/** Log an info exception and a message with optional format args. */
		void info(Throwable t, String message, Object... args);

		/** Log a warning message with optional format args. */
		void warn(String message, Object... args);

		/** Log a warning exception and a message with optional format args. */
		void warn(Throwable t, String message, Object... args);

		/** Log an error message with optional format args. */
		void error(String message, Object... args);

		/** Log an error exception and a message with optional format args. */
		void error(Throwable t, String message, Object... args);
	}

	/** A facade for attaching tags to logging calls. Install instances via {@link #plant} */
	public interface TaggedTree extends Tree
	{
		/** Set a one-time tag for use on the next logging call. */
		void tag(String tag);
	}

	/** A {@link Tree} for debug builds. Automatically infers the tag from the calling class. */
	public static class DebugTree implements TaggedTree
	{
		private String tag = "";

		private long id = 0;

		private static final int MAX_LOG_LENGTH = 4000;

		private static final Pattern ANONYMOUS_CLASS = Pattern.compile("\\$\\d+$");

		private static final ThreadLocal<String> NEXT_TAG = new ThreadLocal<String>();

		public DebugTree(long id, String tag)
		{
			this.tag = tag;
			this.id = id;
		}

		@Override
		public final void tag(String tag)
		{
			NEXT_TAG.set(tag);
		}

		/**
		 * Returns an explicitly set tag for the next log message or {@code null}. Calling this method clears any set tag so it may only be called once.
		 */
		protected final String nextTag()
		{
			String tag = NEXT_TAG.get();
			if (tag != null)
			{
				NEXT_TAG.remove();
			}
			return tag;
		}

		/**
		 * Creates a tag for a log message.
		 * <p>
		 * By default this method will check {@link #nextTag()} for an explicit tag. If there is no explicit tag, the class name of the caller will be used by inspecting the stack
		 * trace of the current thread.
		 * <p>
		 * Note: Do not call {@code super.createTag()} if you override this method. It will produce incorrect results.
		 */
		protected String createTag()
		{
			String messageTag = null;

			/*
			 * String messageTag = nextTag();
			 * 
			 * if (messageTag != null) { return messageTag; }
			 */

			// DO NOT switch this to Thread.getCurrentThread().getStackTrace(). The test will pass
			// because Robolectric runs them on the JVM but on Android the elements are different.
			StackTraceElement[] stackTrace = new Throwable().getStackTrace();
			if (stackTrace.length < 6)
			{
				throw new IllegalStateException(
						"Synthetic stacktrace didn't have enough elements: are you using proguard?");
			}
			messageTag = stackTrace[5].getClassName();
			Matcher m = ANONYMOUS_CLASS.matcher(messageTag);
			if (m.find())
			{
				messageTag = m.replaceAll("");
			}
			return messageTag.substring(messageTag.lastIndexOf('.') + 1);
		}

		private static String maybeFormat(String message, Object... args)
		{
			// If no varargs are supplied, treat it as a request to log the string without formatting.
			String result = message;
			if(args.length > 0)
			{
				try
				{
					result = String.format(message, args);
				}
				catch(Exception e)
				{
					Logger.e("HttpLogger", "Exception on formating request : ", e);
				}
			}
			return result;
		}

		@Override
		public final void verbose(String message, Object... args)
		{
			throwShade(Log.VERBOSE, maybeFormat(message, args), null);
		}

		@Override
		public final void verbose(Throwable t, String message, Object... args)
		{
			throwShade(Log.VERBOSE, maybeFormat(message, args), t);
		}

		@Override
		public final void debug(String message, Object... args)
		{
			throwShade(Log.DEBUG, maybeFormat(message, args), null);
		}

		@Override
		public final void debug(Throwable t, String message, Object... args)
		{
			throwShade(Log.DEBUG, maybeFormat(message, args), t);
		}

		@Override
		public final void info(String message, Object... args)
		{
			throwShade(Log.INFO, maybeFormat(message, args), null);
		}

		@Override
		public final void info(Throwable t, String message, Object... args)
		{
			throwShade(Log.INFO, maybeFormat(message, args), t);
		}

		@Override
		public final void warn(String message, Object... args)
		{
			throwShade(Log.WARN, maybeFormat(message, args), null);
		}

		@Override
		public final void warn(Throwable t, String message, Object... args)
		{
			throwShade(Log.WARN, maybeFormat(message, args), t);
		}

		@Override
		public final void error(String message, Object... args)
		{
			throwShade(Log.ERROR, maybeFormat(message, args), null);
		}

		@Override
		public final void error(Throwable t, String message, Object... args)
		{
			throwShade(Log.ERROR, maybeFormat(message, args), t);
		}

		private void throwShade(int priority, String message, Throwable t)
		{
			if (message == null || message.length() == 0)
			{
				if (t == null)
				{
					return; // Swallow message if it's null and there's no throwable.
				}
				message = Log.getStackTraceString(t);
			}
			else if (t != null)
			{
				message += "\n" + Log.getStackTraceString(t);
			}

			String messageTag = createTag();

			message = messageTag + " : " + message;

			logMessage(priority, tag, message);
		}

		/** Log a message! */
		protected void logMessage(int priority, String tag, String message)
		{
			if (message.length() < MAX_LOG_LENGTH)
			{
				Log.println(priority, tag, message);
				return;
			}

			// Split by line, then ensure each line can fit into Log's maximum length.
			for (int i = 0, length = message.length(); i < length; i++)
			{
				int newline = message.indexOf('\n', i);
				newline = newline != -1 ? newline : length;
				do
				{
					int end = Math.min(newline, i + MAX_LOG_LENGTH);
					Log.println(priority, tag, message.substring(i, end));
					i = end;
				}
				while (i < newline);
			}
		}

		@Override
		public long getId()
		{
			// TODO Auto-generated method stub
			return id;
		}
	}

	/** A {@link Tree} which does nothing. Useful for extending. */
	public static class HollowTree implements Tree
	{
		@Override
		public void verbose(String message, Object... args)
		{
		}

		@Override
		public void verbose(Throwable t, String message, Object... args)
		{
		}

		@Override
		public void debug(String message, Object... args)
		{
		}

		@Override
		public void debug(Throwable t, String message, Object... args)
		{
		}

		@Override
		public void info(String message, Object... args)
		{
		}

		@Override
		public void info(Throwable t, String message, Object... args)
		{
		}

		@Override
		public void warn(String message, Object... args)
		{
		}

		@Override
		public void warn(Throwable t, String message, Object... args)
		{
		}

		@Override
		public void error(String message, Object... args)
		{
		}

		@Override
		public void error(Throwable t, String message, Object... args)
		{
		}

		@Override
		public long getId()
		{
			// TODO Auto-generated method stub
			return 0;
		}
	}

	static
	{

		try
		{
			Application context = HikeMessengerApp.getInstance();
			String packageName = context.getPackageName();
			final int flags = context.getPackageManager().getApplicationInfo(packageName, 0).flags;
			DEBUG = (flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0 ? true : false;
		}
		catch (Exception e)
		{
			Log.e("httpLogger", "Error configuring logger", e);
		}
	}
}
