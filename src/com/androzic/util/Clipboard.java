package com.androzic.util;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.Context;
import android.os.Build;

public final class Clipboard
{

	private static ClipboardImpl IMPL;

	static
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
		{
			IMPL = new ClipboardImplHoneyComb();
		}
		else
		{
			IMPL = new ClipboardImplBase();
		}
	}

	public static void copy(Context context, String text)
	{
		IMPL.copy(context, text);
	}

	public static String paste(Context context)
	{
		return IMPL.paste(context);
	}

	private interface ClipboardImpl
	{
		void copy(Context context, String text);
		String paste(Context context);
	}

	@TargetApi(Build.VERSION_CODES.ECLAIR)
	private static class ClipboardImplBase implements ClipboardImpl
	{

		@SuppressWarnings("deprecation")
		@Override
		public void copy(Context context, String text)
		{
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
	        if (clipboard != null)
	        {
	        	clipboard.setText(text);
	        }
		}

		@SuppressWarnings("deprecation")
		@Override
		public String paste(Context context)
		{
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
	        if (clipboard != null)
	        {
	            return (String) clipboard.getText();
	        }
			return null;
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private static class ClipboardImplHoneyComb implements ClipboardImpl
	{

		@Override
		public void copy(Context context, String text)
		{
			android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText("text", text);
			clipboard.setPrimaryClip(clip);
		}

		@Override
		public String paste(Context context)
		{
	        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
	        if (clipboard != null && clipboard.getPrimaryClip() != null && clipboard.getPrimaryClip().getItemCount() > 0)
	        {
	            return (String) clipboard.getPrimaryClip().getItemAt(0).getText();
	        }
	        return null;
		}
	}

}