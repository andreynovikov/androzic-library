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

package com.androzic.map.mbtiles;

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

import java.util.ArrayList;
import java.util.List;

/**
 * https://github.com/mapbox/mbtiles-spec/blob/master/1.2/spec.md
 */
public class MBTilesMap extends TileMap
{
	private static final long serialVersionUID = 1L;

	public static final byte[] MAGIC = "SQLite format".getBytes();

	private static final String SQL_CREATE_TILES = "CREATE TABLE tiles (zoom_level integer, tile_column integer, tile_row integer, tile_data blob);";
	private static final String SQL_CREATE_METADATA = "CREATE TABLE metadata (name text, value text);";
	private static final String SQL_GET_METADATA = "SELECT * FROM metadata";
	private static final String SQL_GET_IMAGE = "SELECT tile_data FROM tiles WHERE tile_column = ? AND tile_row = ? AND zoom_level = ?";
	private static final String SQL_GET_MINZOOM = "SELECT MIN(zoom_level) FROM tiles";
	private static final String SQL_GET_MAXZOOM = "SELECT MAX(zoom_level) FROM tiles";
	private static final String SQL_GET_MINX = "SELECT MIN(tile_row) FROM tiles WHERE zoom_level = ?";
	private static final String SQL_GET_MINY = "SELECT MIN(tile_row) FROM tiles WHERE zoom_level = ?";
	private static final String SQL_GET_MAXX = "SELECT MAX(tile_column) FROM tiles WHERE zoom_level = ?";
	private static final String SQL_GET_MAXY = "SELECT MAX(tile_column) FROM tiles WHERE zoom_level = ?";

	private transient SQLiteDatabase database;

	protected MBTilesMap()
	{
	}

	public MBTilesMap(String path)
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

			String boundsString = null;
			Cursor c = database.rawQuery(SQL_GET_METADATA, null);
			c.moveToFirst();
			while (!c.isAfterLast())
			{
				/*
				name: The plain-english name of the tileset.
				type: overlay or baselayer
				version: The version of the tileset, as a plain number.
				description: A description of the layer as plain text.
				format: The image file format of the tile data: png or jpg

				bounds: The maximum extent of the rendered map area. Bounds must define an area covered by all zoom levels. The bounds are represented in WGS:84 - latitude and longitude values, in the OpenLayers Bounds format - left, bottom, right, top. Example of the full earth: -180.0,-85,180,85.
				*/
				String n = c.getString(0);
				String v = c.getString(1);
				Log.i("MBTiles", n + ": " + v);
				if ("name".equals(n))
					name = v;
				if ("bounds".endsWith(n))
					boundsString = v;
				c.moveToNext();
			}
			c.close();

			setCornersAmount(4);
			boolean hasCorners = false;
			if (boundsString != null)
			{
				String[] bnds = boundsString.split(",");
				if (bnds.length == 4)
				{
					double left = Double.valueOf(bnds[0]);
					double bottom = Double.valueOf(bnds[1]);
					double right = Double.valueOf(bnds[2]);
					double top = Double.valueOf(bnds[3]);
					cornerMarkers[0].lat = bottom;
					cornerMarkers[0].lon = left;
					cornerMarkers[1].lat = top;
					cornerMarkers[1].lon = left;
					cornerMarkers[2].lat = top;
					cornerMarkers[2].lon = right;
					cornerMarkers[3].lat = bottom;
					cornerMarkers[3].lon = right;
					int[] xy = new int[2];
					for (int i = 0; i < 4; i++)
					{
						getXYByLatLon(cornerMarkers[i].lat, cornerMarkers[i].lon, xy);
						cornerMarkers[i].x = xy[0];
						cornerMarkers[i].y = xy[1];
					}
					hasCorners = true;
				}
			}
			if (!hasCorners)
			{
				String[] args = {String.valueOf(zmax)};
				int minx = getInt(SQL_GET_MINX, args);
				int miny = getInt(SQL_GET_MINY, args);
				int maxx = getInt(SQL_GET_MAXX, args);
				int maxy = getInt(SQL_GET_MAXY, args);
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
			}

			updateTitle();
		}
		catch (SQLException e)
		{
			loadError = e;
		}
		finally
		{
			database.close();
		}
	}

	@Override
	public void destroy()
	{
	}

	@Override
	public synchronized void activate(OnMapTileStateChangeListener listener, double mpp, boolean current) throws Throwable
	{
		database = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY);
		super.activate(listener, mpp, current);
	}

	@Override
	public synchronized void deactivate()
	{
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
			String[] args = {String.valueOf(tx), String.valueOf((int) (Math.pow(2, z) - 1 - ty)), String.valueOf(z)};
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
