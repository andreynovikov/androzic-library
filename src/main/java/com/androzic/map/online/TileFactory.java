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

package com.androzic.map.online;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import com.androzic.BaseApplication;
import com.androzic.map.Tile;
import com.androzic.map.TileRAMCache;

public class TileFactory
{
	public static void downloadTile(TileProvider provider, Tile t)
	{
		String url = provider.getTileUri(t.x, t.y, t.zoomLevel);
		if (url == null)
			return;
		try
		{
			URLConnection c = new URL(url).openConnection();
			c.setConnectTimeout(50000);
			c.connect();
			t.bitmap = BitmapFactory.decodeStream(c.getInputStream());
			if (t.bitmap != null)
			{
				t.generated = false;
				t.expired = false;
				if (provider.listener != null)
					provider.listener.onTileObtained();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void loadTile(TileProvider provider, Tile t)
	{
		BaseApplication application = BaseApplication.getApplication();
		if (application == null)
			return;

		File cache = application.getCacheDir();
		if (cache == null) // cache is not available now
			return;

		File file = getTileFile(cache, provider.code, t.x, t.y, t.zoomLevel);
		if (! file.exists())
			return;
		try
		{
			FileInputStream fileInputStream;
			fileInputStream = new FileInputStream(file);
			byte[] data = new byte[(int) file.length()];
			int count = fileInputStream.read(data);
			fileInputStream.close();
			if (count != data.length)
				return;
			t.bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
			t.expired = provider.tileExpiration > 0 && file.lastModified() + provider.tileExpiration < System.currentTimeMillis();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public static void generateTile(TileProvider provider, TileRAMCache cache, Tile t)
	{
		byte parentTileZoom = (byte) (t.zoomLevel - 1);
		int parentTileX = t.x / 2, parentTileY = t.y / 2, scale = 2;

		// Search for parent tile
		for (; parentTileZoom >= provider.minZoom; parentTileZoom--, parentTileX /= 2, parentTileY /= 2, scale *= 2)
		{
			Tile parentTile = new Tile(parentTileX, parentTileY, parentTileZoom);

			if (cache.containsKey(parentTile.getKey()))
				parentTile = cache.get(parentTile.getKey());
			else
				TileFactory.loadTile(provider, parentTile);

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
				if (provider.listener != null)
					provider.listener.onTileObtained();
				miniTileBitmap.recycle();
				break;
			}
		}
	}

	public static void saveTile(TileProvider provider, byte[] dat, int tx, int ty, byte z)
	{
		BaseApplication application = BaseApplication.getApplication();
		if (application == null)
			return;

		File cache = application.getCacheDir();
		if (cache == null) // cache is not available now
			return;

		File file = getTileFile(cache, provider.code, tx, ty, z);
		//noinspection ResultOfMethodCallIgnored
		file.getParentFile().mkdirs();
		FileOutputStream fileOutputStream;
		try
		{
			fileOutputStream = new FileOutputStream(file);
			fileOutputStream.write(dat);
			fileOutputStream.flush();
			fileOutputStream.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static void saveTile(TileProvider provider, Tile t)
	{
		if (t.bitmap != null && ! t.bitmap.isRecycled())
		{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			t.bitmap.compress(CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
			byte[] data = bos.toByteArray();
			saveTile(provider, data, t.x, t.y, t.zoomLevel);
		}			
	}

	public static File getTileFile(File root, String provider, int x, int y, byte z)
	{
		return new File(root, "tiles"
				+ File.separator + provider
				+ File.separator + "z" + String.valueOf(z)
				+ File.separator + String.valueOf(x / 1024)
				+ File.separator + "x" + String.valueOf(x)
				+ File.separator + String.valueOf(y / 1024)
				+ File.separator + "y" + String.valueOf(y)
				+ ".png");
	}
}
