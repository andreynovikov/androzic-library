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

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;

import com.androzic.data.Bounds;
import com.androzic.map.OnMapTileStateChangeListener;
import com.androzic.map.Tile;
import com.androzic.map.TileMap;

public class OnlineMap extends TileMap
{
	private static final long serialVersionUID = 3L;
	
	public TileProvider tileProvider;
	private transient TileController tileController;

	public OnlineMap(TileProvider provider, byte z)
	{
		super("http://... [" + provider.code + "]");

	    tileProvider = provider;
		srcZoom = z;
	}

	@Override
	public void initialize()
	{
		tileController = new TileController(tileProvider);

		if (srcZoom < tileProvider.minZoom)
			srcZoom = tileProvider.minZoom;
		if (srcZoom > tileProvider.maxZoom)
			srcZoom = tileProvider.maxZoom;

		name = tileProvider.name;
		ellipsoid = tileProvider.ellipsoid;
		minZoom = tileProvider.minZoom;
		maxZoom = tileProvider.maxZoom;

		initializeZooms(tileProvider.minZoom, tileProvider.maxZoom, srcZoom);

		Bounds bounds = new Bounds();
		bounds.minLat = -85.047336;
		bounds.maxLat = 85.051129;
		bounds.minLon = -180;
		bounds.maxLon = 180;

		setCornersAmount(4);
		cornerMarkers[0].lat = bounds.maxLat;
		cornerMarkers[0].lon = bounds.minLon;
		cornerMarkers[1].lat = bounds.maxLat;
		cornerMarkers[1].lon = bounds.maxLon;
		cornerMarkers[2].lat = bounds.minLat;
		cornerMarkers[2].lon = bounds.maxLon;
		cornerMarkers[3].lat = bounds.minLat;
		cornerMarkers[3].lon = bounds.minLon;
		int[] xy = new int[2];
		for (int i = 0; i < 4; i++)
		{
			getXYByLatLon(cornerMarkers[i].lat, cornerMarkers[i].lon, xy);
			cornerMarkers[i].x = xy[0];
			cornerMarkers[i].y = xy[1];
		}

		updateTitle();
	}

	@Override
	public void destroy()
	{
	}

	@Override
	public synchronized void activate(OnMapTileStateChangeListener listener, double mpp, boolean current) throws Throwable
	{
		tileProvider.activate();
		super.activate(listener, mpp, current);
	}
	
	@Override
	public synchronized void deactivate()
	{
		super.deactivate();
		tileController.interrupt();
		tileProvider.deactivate();
	}
	
	@Override
	public boolean containsArea(Bounds area)
	{
		// FIXME disabled online maps in adjacent maps
		return false;
//		return area.minLat < 85.051129 && area.maxLat > -85.047336;
	}

	@Override
	public Bitmap getTile(int x, int y) throws OutOfMemoryError
	{
		Tile tile = tileController.getTile(x, y, srcZoom);
		if (tile.bitmap != null)
		{
			if (dynZoom != 1.0)
			{
		        int ss = (int) (dynZoom * tileSize);
				tile.bitmap = Bitmap.createScaledBitmap(tile.bitmap, ss, ss, true);
			}
		}
		return tile.bitmap;
	}

	@Override
	public synchronized void setZoom(double z)
	{
		super.setZoom(z);
		tileController.reset();
	}

	@Override
	public int getPriority()
	{
		return 4;
	}

	@Override
	public void recalculateCache()
	{
		super.recalculateCache();
		tileController.setCache(cache);
	}

	@Override
	public List<String> info()
	{
		ArrayList<String> info = new ArrayList<>();
		
		info.add("title: " + title);
		if (projection != null)
		{
			info.add("projection: " + projection.getName() + " (" + projection.getEPSGCode() + ")");
			info.add("\t" + projection.getPROJ4Description());
		}
		info.add("datum: " + datum);
		info.add("scale (mpp): " + mpp);
/*
		info.add("calibration points:");
		
		int i = 1;
		for (MapPoint mp : calibrationPoints)
		{
			info.add(String.format("\t%02d: x: %d y: %d lat: %f lon: %f", i, mp.x, mp.y, mp.lat, mp.lon));
			i++;
		}
		double[] ll = new double[2];
		getLatLonByXY(width/2, height/2, ll);
		info.add("map center (calibration) test: "+ll[0] + " " + ll[1]);
		
		info.add("corners:");
*/
	
		return info;
	}
}
