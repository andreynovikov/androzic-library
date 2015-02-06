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

package com.androzic.map.sas;

import android.graphics.Bitmap;

import com.androzic.map.Tile;
import com.androzic.map.TileMap;

import java.util.ArrayList;
import java.util.List;

public class SASMap extends TileMap
{
	private static final long serialVersionUID = 2L;

	public String ext;

	public SASMap(String name, String path, String ext, int zmin, int zmax)
	{
		super(path);

		this.name = name;
		this.ext = ext;

		initializeZooms((byte) zmin, (byte) zmax, (byte) zmax);
		updateTitle();
		recalculateMPP();
	}

	@Override
	public void initialize()
	{
	}

	@Override
	public void destroy()
	{
	}

	@Override
	public Bitmap getTile(int x, int y) throws OutOfMemoryError
	{
		try
		{
			// SAS counts zooms from 1, not from 0
			long key = Tile.getKey(x, y, (byte) (srcZoom + 1));
			Tile tile = cache.get(key);
			if (tile == null)
			{
				tile = new Tile(x, y, (byte) (srcZoom + 1));
				SASTileFactory.loadTile(this, tile);
				if (tile.bitmap == null)
				{
					SASTileFactory.generateTile(this, cache, tile);
				}
				if (tile.bitmap != null)
				{
					if (dynZoom != 1.0)
					{
				        int ss = (int) (dynZoom * TILE_SIZE);
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
		info.add("tile extension: " + ext);
		if (projection != null)
		{
			info.add("projection: " + projection.getName() + " (" + projection.getEPSGCode() + ")");
			info.add("\t" + projection.getPROJ4Description());
		}
		info.add("datum: " + datum);
		info.add("scale (mpp): " + mpp);
	
		return info;
	}

}
