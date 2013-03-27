/*
 * Androzic - android navigation client that uses OziExplorer maps (ozf2, ozfx3).
 * Copyright (C) 2010-2012 Andrey Novikov <http://andreynovikov.info/>
 * 
 * This file is part of Androzic application.
 * 
 * Androzic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Androzic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Androzic. If not, see <http://www.gnu.org/licenses/>.
 */

package com.androzic;

import android.app.Application;
import android.os.Build;

public abstract class BaseApplication extends Application
{
	private static BaseApplication self;

	@SuppressWarnings("unchecked")
	public static <T extends BaseApplication> T getApplication()
	{
		return (T) self;
	}

	protected static <T extends BaseApplication> void setInstance(T instance)
	{
		self = instance;
	}

	public abstract String getRootPath();

	/**
	 * Returns device name in user-friendly format
	 */
	public static String getDeviceName()
	{
		String manufacturer = Build.MANUFACTURER;
		String model = Build.MODEL;
		if (model.startsWith(manufacturer))
			return capitalize(model);
		else
			return capitalize(manufacturer) + " " + model;
	}

	private static String capitalize(String s)
	{
		if (s == null || s.length() == 0)
			return "";
		char first = s.charAt(0);
		if (Character.isUpperCase(first))
			return s;
		else
			return Character.toUpperCase(first) + s.substring(1);
	}
}
