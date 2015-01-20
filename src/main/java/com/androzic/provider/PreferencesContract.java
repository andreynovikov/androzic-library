/*
 * Androzic - android navigation client that uses OziExplorer maps (ozf2, ozfx3).
 * Copyright (C) 2010-2015  Andrey Novikov <http://andreynovikov.info/>
 *
 * This file is part of Androzic application.
 *
 * Androzic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * Androzic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with Androzic.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.androzic.provider;

import android.net.Uri;

public final class PreferencesContract
{
	public static final String AUTHORITY = "com.androzic.PreferencesProvider";
	protected static final String PATH = "preferences";
	public static final Uri PREFERENCES_URI;
	
	public static final String[] DATA_COLUMNS = new String[] {"VALUE"};
	public static final int DATA_COLUMN = 0;
	public static final String DATA_SELECTION = "IDLIST";
	
	/**
	 * double
	 */
	public static final int SPEED_FACTOR = 1;
	/**
	 * String
	 */
	public static final int SPEED_ABBREVIATION = 2;
	/**
	 * double
	 */
	public static final int DISTANCE_FACTOR = 3;
	/**
	 * String
	 */
	public static final int DISTANCE_ABBREVIATION = 4;
	/**
	 * double
	 */
	public static final int DISTANCE_SHORT_FACTOR = 5;
	/**
	 * String
	 */
	public static final int DISTANCE_SHORT_ABBREVIATION = 6;
	/**
	 * double
	 */
	public static final int ELEVATION_FACTOR = 7;
	/**
	 * String
	 */
	public static final int ELEVATION_ABBREVIATION = 8;
	/**
	 * int
	 */
	public static final int COORDINATES_FORMAT = 9;
	
	static
	{
		PREFERENCES_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH);
	}
}
