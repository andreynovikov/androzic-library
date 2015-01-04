package com.androzic.provider;

import android.net.Uri;

public final class DataContract
{
	public static final String AUTHORITY = "com.androzic.DataProvider";
	public static final String ACTION_PICK_ICON = "com.androzic.PICK_ICON";
	public static final String ACTION_PICK_MARKER = "com.androzic.PICK_MARKER";

	protected static final String MAPOBJECTS_PATH = "mapobjects";
	public static final Uri MAPOBJECTS_URI = Uri.parse("content://" + AUTHORITY + "/" + MAPOBJECTS_PATH);
	protected static final String ICONS_PATH = "icons";
	public static final Uri ICONS_URI = Uri.parse("content://" + AUTHORITY + "/" + ICONS_PATH);
	protected static final String MARKERS_PATH = "markers";
	public static final Uri MARKERS_URI = Uri.parse("content://" + AUTHORITY + "/" + MARKERS_PATH);

	public static final String[] MAPOBJECT_COLUMNS = new String[] {"latitude", "longitude", "bitmap", "name", "description", "image", "marker", "textcolor", "backcolor"};
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
	/**
	 * Image marker, from markers pack (String, optional)
	 */
	public static final int MAPOBJECT_MARKER_COLUMN = 6;
	/**
	 * Text color (int, optional)
	 */
	public static final int MAPOBJECT_TEXTCOLOR_COLUMN = 7;
	/**
	 * Marker/background color (int, optional)
	 */
	public static final int MAPOBJECT_BACKCOLOR_COLUMN = 8;

	public static final String MAPOBJECT_ID_SELECTION = "IDLIST";
	
	public static final String[] ICON_COLUMNS = new String[] {"BITMAP"};
	public static final int ICON_COLUMN = 0;

	public static final String[] MARKER_COLUMNS = new String[] {"BITMAP"};
	public static final int MARKER_COLUMN = 0;
}
