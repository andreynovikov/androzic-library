package com.androzic.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils
{
	public static String unusable = "*+~|<>!?\\/:";

	/**
	 * Replace illegal characters in a filename with "_" Illegal characters: : \
	 * / * ? | < >
	 * 
	 * @param name
	 * @return sanitized string
	 */
	public static String sanitizeFilename(String name)
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < name.length(); i++)
		{
			if (unusable.indexOf(name.charAt(i)) > -1)
				sb.append("_");
			else
				sb.append(name.charAt(i));
		}
		return sb.toString();
	}

	public static void copyFile(File from, File to) throws IOException
	{
		//create output directory if it doesn't exist
		to.getParentFile().mkdirs();

		InputStream in = new FileInputStream(from);
		OutputStream out = new FileOutputStream(to);

		byte[] buffer = new byte[1024];
		int read;
		while ((read = in.read(buffer)) != -1)
			out.write(buffer, 0, read);
		in.close();

		// write the output file
		out.flush();
		out.close();
	}
}