package com.androzic;

import android.text.TextUtils;

public final class Log {

   static int logMode = 1;
   
   final static int LOG_MODE_FULL = 1;
   final static int LOG_MODE_LIGHT = 2;
   final static int LOG_MODE_NONE = 3;
   
   public static void w(String TAG, String msg) {
	   switch (logMode ){
	   	case LOG_MODE_FULL:
		   android.util.Log.w(TAG, getLocation() + msg);
	   	case LOG_MODE_LIGHT:
			   android.util.Log.w(TAG, msg);   
	   }
   }
   
   private static String getLocation() {
       final String className = Log.class.getName();
       final StackTraceElement[] traces = Thread.currentThread().getStackTrace();
       boolean found = false;

       for (int i = 0; i < traces.length; i++) {
           StackTraceElement trace = traces[i];

           try {
               if (found) {
                   if (!trace.getClassName().startsWith(className)) {
                       Class<?> clazz = Class.forName(trace.getClassName());
                       return "[" + getClassName(clazz) + ":" + trace.getMethodName() + ":" + trace.getLineNumber() + "]: ";
                   }
               }
               else if (trace.getClassName().startsWith(className)) {
                   found = true;
                   continue;
               }
           }
           catch (ClassNotFoundException e) {
           }
       }

       return "[]: ";
   }

   private static String getClassName(Class<?> clazz) {
       if (clazz != null) {
           if (!TextUtils.isEmpty(clazz.getSimpleName())) {
               return clazz.getSimpleName();
           }

           return getClassName(clazz.getEnclosingClass());
       }

       return "";
   }
}
