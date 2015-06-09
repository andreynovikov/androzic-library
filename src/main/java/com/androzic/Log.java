package com.androzic;

import android.text.TextUtils;

public final class Log
{
	static int logLevel = android.util.Log.ERROR;
	static int logMode = 2;

	final static int LOG_MODE_FULL = 1;
	final static int LOG_MODE_LIGHT = 2;
	final static int LOG_MODE_NONE = 3;

	public static void d(String TAG, String msg)
	{
		if (logLevel <= android.util.Log.DEBUG)
			log(android.util.Log.DEBUG, TAG, msg);
	}

	public static void i(String TAG, String msg)
	{
		if (logLevel <= android.util.Log.INFO)
			log(android.util.Log.INFO, TAG, msg);
	}

	public static void w(String TAG, String msg)
	{
		if (logLevel <= android.util.Log.WARN)
			log(android.util.Log.WARN, TAG, msg);
	}

	public static void e(String TAG, String msg)
	{
		if (logLevel <= android.util.Log.ERROR)
			log(android.util.Log.ERROR, TAG, msg);
	}

	public static void log(int priority, String TAG, String msg)
	{
		switch (logMode)
		{
			case LOG_MODE_FULL:
				android.util.Log.println(priority, TAG, getLocation() + msg);
			case LOG_MODE_LIGHT:
				android.util.Log.println(priority, TAG, msg);
			case LOG_MODE_NONE:
		}
	}

	private static String getLocation()
	{
		final String className = Log.class.getName();
		final StackTraceElement[] traces = Thread.currentThread()
				.getStackTrace();
		boolean found = false;

		for (StackTraceElement trace : traces)
		{
			try
			{
				if (found)
				{
					if (!trace.getClassName().startsWith(className))
					{
						Class<?> clazz = Class.forName(trace.getClassName());
						return "[" + getClassName(clazz) + ":"
								+ trace.getMethodName() + ":"
								+ trace.getLineNumber() + "]: ";
					}
				}
				else if (trace.getClassName().startsWith(className))
				{
					found = true;
				}
			}
			catch (ClassNotFoundException e)
			{
				//ignore
			}
		}

		return "[]: ";
	}

	private static String getClassName(Class<?> clazz)
	{
		if (clazz != null) {
			if (!TextUtils.isEmpty(clazz.getSimpleName()))
			{
				return clazz.getSimpleName();
			}

			return getClassName(clazz.getEnclosingClass());
		}

		return "";
	}
}
