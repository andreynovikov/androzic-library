package com.androzic.provider;

import android.net.Uri;

public final class DataContract
{
	public static final String AUTHORITY = "com.androzic.DataProvider";
	protected static final String MAPOBJECTS_PATH = "mapobjects";
	public static final Uri MAPOBJECTS_URI = Uri.parse("content://" + AUTHORITY + "/" + MAPOBJECTS_PATH);
	
	public static final String[] MAPOBJECT_COLUMNS = new String[] {"latitude", "longitude", "bitmap", "name", "description", "image"};
	/**
	 * Latitude (double, required)
	 */
	public static final int MAPOBJECT_LATITUDE_COLUMN = 0;
	/**
	 * Longitude (double, required)
	 */
	public static final int MAPOBJECT_LONGITUDE_COLUMN = 1;
	/**
	 * Bitmap (ByteArray, required if name is not provided)
	 */
	public static final int MAPOBJECT_BITMAP_COLUMN = 2;
	/**
	 * Name (String, required if bitmap is not provided)
	 */
	public static final int MAPOBJECT_NAME_COLUMN = 3;
	/**
	 * Description (String, optional)
	 */
	public static final int MAPOBJECT_DESCRIPTION_COLUMN = 4;
	/**
	 * Image name, from icons pack (String, optional)
	 */
	public static final int MAPOBJECT_IMAGE_COLUMN = 5;
	public static final String MAPOBJECT_ID_SELECTION = "IDLIST";
}
