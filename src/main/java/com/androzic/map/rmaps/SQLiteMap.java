/*
 * Androzic - android navigation client that uses OziExplorer maps (ozf2, ozfx3).
 * Copyright (C) 2010-2015 Andrey Novikov <http://andreynovikov.info/>
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

package com.androzic.map.rmaps;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import com.androzic.Log;
import com.androzic.map.OnMapTileStateChangeListener;
import com.androzic.map.Tile;
import com.androzic.map.TileMap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SQLiteMap extends TileMap
{
	private static final long serialVersionUID = 1L;

	public static final byte[] MAGIC = "SQLite format".getBytes();

	private static final String SQL_CREATE_tiles = "CREATE TABLE IF NOT EXISTS tiles (x int, y int, z int, s int, image blob, PRIMARY KEY (x,y,z,s));";
	private static final String SQL_CREATE_info = "CREATE TABLE IF NOT EXISTS info (maxzoom Int, minzoom Int, params VARCHAR);";
	private static final String SQL_SELECT_PARAMS = "SELECT * FROM info";
	private static final String SQL_GET_IMAGE = "SELECT image FROM tiles WHERE x = ? AND y = ? AND z = ?";
	private static final String SQL_TILES_COUNT = "SELECT COUNT(*) cnt FROM tiles";
	private static final String SQL_GET_MINZOOM = "SELECT DISTINCT 17 - z FROM tiles ORDER BY z DESC LIMIT 1;";
	private static final String SQL_GET_MAXZOOM = "SELECT DISTINCT 17 - z FROM tiles ORDER BY z ASC LIMIT 1;";
	private static final String SQL_GET_MINX = "SELECT MIN(x) FROM tiles WHERE z = ?";
	private static final String SQL_GET_MINY = "SELECT MIN(y) FROM tiles WHERE z = ?";
	private static final String SQL_GET_MAXX = "SELECT MAX(x) FROM tiles WHERE z = ?";
	private static final String SQL_GET_MAXY = "SELECT MAX(y) FROM tiles WHERE z = ?";

	private transient SQLiteDatabase database;

	protected SQLiteMap()
	{
	}

	public SQLiteMap(String path)
	{
		super(path);
	}

	@Override
	public void initialize()
	{
		try
		{
			database = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY);
			byte zmin = (byte) database.compileStatement(SQL_GET_MINZOOM).simpleQueryForLong();
			byte zmax = (byte) database.compileStatement(SQL_GET_MAXZOOM).simpleQueryForLong();
			initializeZooms(zmin, zmax, zmax);

			// Remove extension
			File file = new File(path);
			name = file.getName().toLowerCase();
			int e = name.lastIndexOf(".sqlitedb");
			if (e > 0)
				name = name.substring(0, e);

			// And capitalize first letter
			StringBuilder nameSb = new StringBuilder(name);
			nameSb.setCharAt(0, Character.toUpperCase(nameSb.charAt(0)));
			name = nameSb.toString();

			String[] args = {String.valueOf(17 - zmax)};
			int minx = getInt(SQL_GET_MINX, args);
			int miny = getInt(SQL_GET_MINY, args);
			int maxx = getInt(SQL_GET_MAXX, args);
			int maxy = getInt(SQL_GET_MAXY, args);
			setCornersAmount(4);
			cornerMarkers[0].x = minx * TILE_SIZE;
			cornerMarkers[0].y = miny * TILE_SIZE;
			cornerMarkers[1].x = minx * TILE_SIZE;
			cornerMarkers[1].y = (maxy + 1) * TILE_SIZE;
			cornerMarkers[2].x = (maxx + 1) * TILE_SIZE;
			cornerMarkers[2].y = (maxy + 1) * TILE_SIZE;
			cornerMarkers[3].x = (maxx + 1) * TILE_SIZE;
			cornerMarkers[3].y = miny * TILE_SIZE;
			double[] ll = new double[2];
			for (int i = 0; i < 4; i++)
			{
				getLatLonByXY(cornerMarkers[i].x, cornerMarkers[i].y, ll);
				cornerMarkers[i].lat = ll[0];
				cornerMarkers[i].lon = ll[1];
			}
			database.close();

			updateTitle();
		}
		catch (SQLException e)
		{
			loadError = e;
		}
	}

	@Override
	public void destroy()
	{
	}

	@Override
	public synchronized void activate(OnMapTileStateChangeListener listener, double mpp, boolean current) throws Throwable
	{
		Log.e("SQLMap", "activate(): " + name);
		database = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY);
		super.activate(listener, mpp, current);
	}

	@Override
	public synchronized void deactivate()
	{
		Log.e("SQLMap", "deactivate(): " + name);
		super.deactivate();
		database.close();
	}

	@Override
	public int getPriority()
	{
		return 3;
	}

	@Override
	public Bitmap getTile(int x, int y) throws OutOfMemoryError
	{
		try
		{
			long key = Tile.getKey(x, y, srcZoom);
			Tile tile = cache.get(key);
			if (tile == null)
			{
				tile = new Tile(x, y, srcZoom);
				loadTile(tile);
				if (tile.bitmap == null)
				{
					generateTile(tile);
				}
				if (tile.bitmap != null)
				{
					if (dynZoom != 1.0)
					{
				        int ss = (int) (dynZoom * tileSize);
						tile.bitmap = Bitmap.createScaledBitmap(tile.bitmap, ss, ss, true);
					}
					cache.put(tile.getKey(), tile);
				}
			}
			return tile.bitmap;
		}
		catch (NullPointerException e)
		{
			//TODO Strange situation when cache becomes null in the middle of the method
			return null;
		}
	}

	@Override
	public List<String> info()
	{
		ArrayList<String> info = new ArrayList<>();
		
		info.add("title: " + title);
		info.add("path: " + path);
		info.add("minimum zoom: " + minZoom);
		info.add("maximum zoom: " + maxZoom);
		if (projection != null)
		{
			info.add("projection: " + projection.getName() + " (" + projection.getEPSGCode() + ")");
			info.add("\t" + projection.getPROJ4Description());
		}
		info.add("datum: " + datum);
		info.add("scale (mpp): " + mpp);
	
		return info;
	}

	private int getInt(String query, String[] args)
	{
		int r = 0;
		Cursor c = database.rawQuery(query, args);
		try
		{
			c.moveToFirst();
			r = c.getInt(0);
			return r;
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
		finally
		{
			c.close();
		}
		return r;
	}

	public byte[] loadTile(int tx, int ty, byte z)
	{
		byte[] data = null;

		if (database.isOpen())
		{
			String[] args = {String.valueOf(tx), String.valueOf(ty), String.valueOf(17 - z)};
			Cursor c = database.rawQuery(SQL_GET_IMAGE, args);
			try
			{
				c.moveToFirst();
				data = c.getBlob(0);
			}
			catch (Throwable ignore)
			{
			}
			finally
			{
				c.close();
			}
		}
		return data;
	}

	public void loadTile(Tile t)
	{
		byte[] data = loadTile(t.x, t.y, t.zoomLevel);
		if (data != null)
			t.bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
	}

	public void generateTile(Tile t)
	{
		byte parentTileZoom = (byte) (t.zoomLevel - 1);
		int parentTileX = t.x / 2, parentTileY = t.y / 2, scale = 2;

		// Search for parent tile
		for (; parentTileZoom >= 0; parentTileZoom--, parentTileX /= 2, parentTileY /= 2, scale *= 2)
		{
			Tile parentTile = new Tile(parentTileX, parentTileY, parentTileZoom);

			if (cache.containsKey(parentTile.getKey()))
				parentTile = cache.get(parentTile.getKey());
			else
				loadTile(parentTile);

			if (parentTile.bitmap != null && scale <= parentTile.bitmap.getWidth() && scale <= parentTile.bitmap.getHeight())
			{
				Matrix matrix = new Matrix();
				matrix.postScale(scale, scale);

				int miniTileWidth = parentTile.bitmap.getWidth() / scale;
				int miniTileHeight = parentTile.bitmap.getHeight() / scale;
				int fromX = (t.x % scale) * miniTileWidth;
				int fromY = (t.y % scale) * miniTileHeight;

				// Create mini bitmap which will be stretched to tile
				Bitmap miniTileBitmap = Bitmap.createBitmap(parentTile.bitmap, fromX, fromY, miniTileWidth, miniTileHeight);

				// Create tile bitmap from mini bitmap
				t.bitmap = Bitmap.createBitmap(miniTileBitmap, 0, 0, miniTileWidth, miniTileHeight, matrix, false);
				t.generated = true;
				miniTileBitmap.recycle();
				break;
			}
		}
	}

}
