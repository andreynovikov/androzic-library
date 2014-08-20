/*
 * Androzic - android navigation client that uses OziExplorer maps (ozf2, ozfx3).
 * Copyright (C) 2010-2014  Andrey Novikov <http://andreynovikov.info/>
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

package com.androzic.map.sas;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import com.androzic.BaseApplication;
import com.androzic.map.Tile;
import com.androzic.map.TileRAMCache;

public class SASTileFactory
{
	public static byte[] loadTile(SASMap map, int tx, int ty, byte z)
	{
		BaseApplication application = BaseApplication.getApplication();
		if (application == null)
			return null;
		
		File file = new File(getTilePath(map.path, map.ext, tx, ty, z));
		if (file.exists() == false)
			return null;
		try
		{
			FileInputStream fileInputStream;
			fileInputStream = new FileInputStream(file);
			byte[] dat = new byte[(int) file.length()];
			fileInputStream.read(dat);
			fileInputStream.close();
			return dat;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public static void loadTile(SASMap map, Tile t)
	{
		byte[] data = loadTile(map, t.x, t.y, t.zoomLevel);
		if (data != null)
			t.bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
	}
	
	public static void generateTile(SASMap map, TileRAMCache cache, Tile t)
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
				SASTileFactory.loadTile(map, parentTile);

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

	public static String getTilePath(String path, String ext, int x, int y, byte z)
	{
		return path + File.separator + "z" + String.valueOf(z)
				    + File.separator + String.valueOf(x / 1024)
				    + File.separator + "x" + String.valueOf(x)
				    + File.separator + String.valueOf(y / 1024)
				    + File.separator + "y" + String.valueOf(y)
				    + ext;
	}
}
