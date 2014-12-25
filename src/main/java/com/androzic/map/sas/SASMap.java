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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.util.DisplayMetrics;

import com.androzic.Log;
import com.androzic.map.Map;
import com.androzic.map.Tile;
import com.androzic.map.TileRAMCache;
import com.jhlabs.map.Ellipsoid;
import com.jhlabs.map.proj.ProjectionFactory;

public class SASMap extends Map
{
	private static final long serialVersionUID = 1L;

	public static final int TILE_WIDTH = 256;
	public static final int TILE_HEIGHT = 256;

	private boolean isActive = false;
	private byte srcZoom;
	private byte defZoom;
	private double dynZoom;

	public String path;
	public String ext;
	public String name;
	public byte minZoom;
	public byte maxZoom;
	public boolean ellipsoid = false;

	public SASMap(String name, String path, String ext, int zmin, int zmax)
	{
		super(name);

		this.name = name;
		this.path = path;
		this.ext = ext;
		minZoom = (byte) zmin;
		maxZoom = (byte) zmax;
		
		datum = "WGS84";
		projection = ProjectionFactory.fromPROJ4Specification("+proj=merc".split(" "));
		projection.setEllipsoid(Ellipsoid.WGS_1984);
	    projection.initialize();
	    
	    title = String.format("%s (%d)", name, maxZoom);
		srcZoom = maxZoom;
		defZoom = maxZoom;
		zoom = 1.0;
		dynZoom = 1.0;

	    /*
	     * The distance represented by one pixel (S) is given by
	     * S=C*cos(y)/2^(z+8) 
	     *
	     * where...
	     *
	     * C is the (equatorial) circumference of the Earth 
	     * z is the zoom level 
	     * y is the latitude of where you're interested in the scale. 
	     *
	     * Make sure your calculator is in degrees mode, unless you want to express latitude
	     * in radians for some reason. C should be expressed in whatever scale unit you're
	     * interested in (miles, meters, feet, smoots, whatever). Since the earth is actually
	     * ellipsoidal, there will be a slight error in this calculation. But it's very slight.
	     * (0.3% maximum error) 
	     */
		mpp = projection.getEllipsoid().equatorRadius * Math.PI * 2 * Math.cos(0) / Math.pow(2.0, (srcZoom + 8));
	}

	@Override
	public synchronized void activate(DisplayMetrics metrics, double zoom) throws IOException, OutOfMemoryError
	{
		displayWidth = metrics.widthPixels;
		displayHeight = metrics.heightPixels;

		mapClipPath = new Path();

		if (zoom != 1.)
		{
			if (savedZoom == 0.)
				savedZoom = this.zoom;
			this.zoom = zoom;
		}
		else if (savedZoom != 0.)
		{
			this.zoom = savedZoom;
			savedZoom = 0.;
		}
		setZoom(this.zoom);

		borderPaint = new Paint();
        borderPaint.setAntiAlias(true);
        borderPaint.setStrokeWidth(3);
        borderPaint.setColor(Color.RED);
        borderPaint.setAlpha(128);
        borderPaint.setStyle(Style.STROKE);

		isActive = true;
	}
	
	@Override
	public synchronized void deactivate()
	{
		isActive = false;
		if (cache != null)
			cache.destroy();
		if (savedZoom != 0.)
			zoom = savedZoom;
		savedZoom = 0.;
		cache = null;
		mapClipPath = null;
		borderPaint = null;
	}
	
	public boolean activated()
	{
		return isActive;
	}
	
	@Override
	public boolean drawMap(double[] loc, int[] lookAhead, int width, int height, boolean cropBorder, boolean drawBorder, Canvas c) throws OutOfMemoryError
	{
		if (isActive == false)
			return false;

		int[] map_xy = new int[2];
		getXYByLatLon(loc[0], loc[1], map_xy);
		map_xy[0] -= lookAhead[0];
		map_xy[1] -= lookAhead[1];
		
		Path clipPath = new Path();

		if (cropBorder || drawBorder)
			mapClipPath.offset(-map_xy[0] + width / 2, -map_xy[1] + height / 2, clipPath);

		float tile_w = (float) (TILE_WIDTH * dynZoom);
		float tile_h = (float) (TILE_HEIGHT * dynZoom);

		int sas_x = (int) (map_xy[0] / tile_w);
		int sas_y = (int) (map_xy[1] / tile_h);

		int tiles_per_x = Math.round(width * 1.f / tile_w / 2 + .5f);
		int tiles_per_y = Math.round(height * 1.f / tile_h / 2 + .5f);

		int c_min = sas_x - tiles_per_x;
		int c_max = sas_x + tiles_per_x + 1;
		
		int r_min = sas_y - tiles_per_y;
		int r_max = sas_y + tiles_per_y + 1;
		
		boolean result = true;

		if (c_min < 0)
		{
			c_min = 0;
			result = false;
		}
		if (r_min < 0)
		{
			r_min = 0;
			result = false;
		}
		if (c_max > Math.pow(2.0, srcZoom))
		{
			c_max = (int) (Math.pow(2.0, srcZoom));
			result = false;
		}
		if (r_max > Math.pow(2.0, srcZoom))
		{
			r_max = (int) (Math.pow(2.0, srcZoom));
			result = false;
		}

		float w2mx = width / 2 - map_xy[0];
		float h2my = height / 2 - map_xy[1];
		
		for (int i = r_min; i < r_max; i++)
		{
			for (int j = c_min; j < c_max; j++)
			{
				Bitmap tile = getTile(j, i);

				if (tile != null && ! tile.isRecycled())
				{
					float tx = w2mx + j * tile_w;
					float ty = h2my + i * tile_h;
					c.drawBitmap(tile, tx, ty, null);
				}
				else
				{
					result = false;
				}
			}
		}

		if (drawBorder && borderPaint != null)
			c.drawPath(clipPath, borderPaint);

		return result;
	}

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
				        int sw = (int) (dynZoom * TILE_WIDTH);
				        int sh = (int) (dynZoom * TILE_HEIGHT);
						Bitmap scaled = Bitmap.createScaledBitmap(tile.bitmap, sw, sh, true);
						tile.bitmap = scaled;
					}
					cache.put(tile);
				}
			}
			return tile.bitmap;
		}
		catch (NullPointerException e)
		{
			// Strange situation when cache becomes null in the middle of the method
			return null;
		}
	}
	
	/**
	 * Calculates the inverse hyperbolic tangent of the number, i.e.
	 * the value whose hyperbolic tangent is number  
	 * @param arg number
	 * @return inverse hyperbolic tangent
	 */
	private static double atanh (double arg)
	{
	    return 0.5 * Math.log((1 + arg) / (1 - arg));
	}
	
	@Override
	public boolean getLatLonByXY(int x, int y, double[] ll)
	{
		int map_x = (int) (x * 1. / dynZoom);
		int map_y = (int) (y * 1. / dynZoom);
		double dx = map_x * 1. / TILE_WIDTH;
		double dy = map_y * 1. / TILE_HEIGHT;
		
		double n = Math.pow(2.0, srcZoom);
		if (ellipsoid)
		{
			ll[0] = (map_y-TILE_HEIGHT*n/2)/-(TILE_HEIGHT*n/(2*Math.PI));
			ll[0] = (2*Math.atan(Math.exp(ll[0]))-Math.PI/2)*180/Math.PI;

			double Zu = Math.toRadians(ll[0]);
			double Zum1 = Zu+1;
			double yy = (map_y-TILE_HEIGHT*n/2);
			int i=100000;
			while ((Math.abs(Zum1-Zu)>0.0000001)&&(i!=0))
			{
			  i--;
			  Zum1 = Zu;
			  Zu = Math.asin(1-((1+Math.sin(Zum1))*Math.pow(1-0.0818197*Math.sin(Zum1),0.0818197))
			  /(Math.exp((2*yy)/-(TILE_HEIGHT*n/(2*Math.PI)))*Math.pow(1+0.0818197*Math.sin(Zum1),0.0818197)));
			}
			ll[0]=Math.toDegrees(Zu);
		}
		else
		{
			ll[0] = Math.toDegrees(Math.atan((Math.sinh(Math.PI * (1 - 2 * dy / n)))));
		}
		ll[1] = dx * 360.0 / n - 180.0;		
		
		return true;
	}

	@Override
	public boolean getXYByLatLon(double lat, double lon, int[] xy)
	{
		double n = Math.pow(2.0, srcZoom);
		
		xy[0] = (int) Math.floor((lon + 180.0) / 360.0 * n * TILE_WIDTH * dynZoom);

		if (ellipsoid)
		{
			double z = Math.sin(Math.toRadians(lat));
			xy[1] = (int) Math.floor((1 - (atanh(z)-0.0818197*atanh(0.0818197*z)) / Math.PI) / 2 * n * TILE_HEIGHT * dynZoom);
		}
		else
		{
			xy[1] = (int) Math.floor((1 - (Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI)) / 2 * n * TILE_HEIGHT * dynZoom);
		}
		return true;
	}

	@Override
	public void recalculateCache()
	{
		if (cache != null)
			cache.destroy();
		int nx = (int) Math.ceil(displayWidth * 1. / (TILE_WIDTH * dynZoom)) + 2;
		int ny = (int) Math.ceil(displayHeight * 1. / (TILE_HEIGHT * dynZoom)) + 2;
		int cacheSize = nx * ny;
		Log.e("SAS", "Cache size: " + cacheSize);
		cache = new TileRAMCache(cacheSize);
	}

	@Override
	public double getNextZoom()
	{
		int z = defZoom + (int) (Math.log(zoom) / Math.log(2));
		if (z - maxZoom > 1)
			return 0.0;
		else if (z < minZoom || z > maxZoom)
			return zoom * 2;
		else
			return Math.pow(2, srcZoom + 1 - defZoom);
	}

	@Override
	public double getPrevZoom()
	{
		int z = defZoom + (int) (Math.log(zoom) / Math.log(2));
		if (z - minZoom < -1)
			return 0.0;
		else if (z < minZoom || z > maxZoom)
			return zoom / 2;
		else
			return Math.pow(2, srcZoom - 1 - defZoom);
	}

	@Override
	public double getZoom()
	{
		return zoom;
	}

	@Override
	public synchronized void setZoom(double z)
	{
		int zDiff = (int) (Math.log(z) / Math.log(2));
		Log.e("SAS", "Zoom: " + z + " diff: " + zDiff);

		srcZoom = (byte) (defZoom + zDiff);
		
		if (srcZoom > maxZoom)
		{
			zDiff -= srcZoom - maxZoom;
			srcZoom = maxZoom;
		}
		if (srcZoom < minZoom)
		{
			zDiff -= srcZoom - minZoom;
			srcZoom = minZoom;
		}

		zoom = z;
		dynZoom = zoom / Math.pow(2, srcZoom - defZoom);
		if (Math.abs(dynZoom - 1) < 0.0078125)
			dynZoom = 1.0;
		Log.e("SAS", "z: " + srcZoom + " diff: " + zDiff + " zoom: " + zoom + " dymZoom: " + dynZoom);

		recalculateCache();

	    title = String.format("%s (%d)", name, srcZoom);
	    
		mapClipPath.rewind();
		mapClipPath.setLastPoint((float) (cornerMarkers[0].x * zoom), (float) (cornerMarkers[0].y * zoom));
		for (int i = 1; i < cornerMarkers.length; i++)
			mapClipPath.lineTo((float) (cornerMarkers[i].x * zoom), (float) (cornerMarkers[i].y * zoom));
		mapClipPath.close();
	}

	public int getScaledWidth()
	{
		return (int) (Math.pow(2.0, srcZoom) * TILE_WIDTH * zoom);
	}

	public int getScaledHeight()
	{
		return (int) (Math.pow(2.0, srcZoom) * TILE_HEIGHT * zoom);
	}
	
	public void getMapCenter(double[] center)
	{
		int x = (int) ((cornerMarkers[2].x + cornerMarkers[0].x) / 2 * zoom);
		int y = (int) ((cornerMarkers[2].y + cornerMarkers[0].y) / 2 * zoom);
		getLatLonByXY(x, y, center);
	}
	
	public List<String> info()
	{
		ArrayList<String> info = new ArrayList<String>();
		
		info.add("title: " + title);
		info.add("path: " + path);
		info.add("minimum zoom: " + minZoom);
		info.add("maximum zoom: " + maxZoom);
		info.add("tile extention: " + ext);
		if (projection != null)
		{
			info.add("projection: " + prjName + " (" + projection.getEPSGCode() + ")");
			info.add("\t" + projection.getPROJ4Description());
		}
		info.add("datum: " + datum);
		info.add("scale (mpp): " + mpp);
	
		return info;
	}

}
