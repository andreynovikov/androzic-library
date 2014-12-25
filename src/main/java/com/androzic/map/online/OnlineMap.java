/*
 * Androzic - android navigation client that uses OziExplorer maps (ozf2, ozfx3).
 * Copyright (C) 2010-2012  Andrey Novikov <http://andreynovikov.info/>
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.DisplayMetrics;

import com.androzic.Log;
import com.androzic.data.Bounds;
import com.androzic.map.Map;
import com.androzic.map.Tile;
import com.androzic.map.TileRAMCache;
import com.jhlabs.map.Ellipsoid;
import com.jhlabs.map.proj.ProjectionFactory;

public class OnlineMap extends Map
{
	private static final long serialVersionUID = 2L;
	
	private static final int ORIG_TILE_WIDTH = 256;
	private static final int ORIG_TILE_HEIGHT = 256;
	public static int TILE_WIDTH = 256;
	public static int TILE_HEIGHT = 256;
	
	private static double prescaleFactor = 1.;
	
	private TileController tileController;
	private TileProvider tileProvider;
	private boolean isActive = false;
	private byte srcZoom;
	private byte defZoom;
	private double dynZoom;
	
	private double lastLatitude;
	
	public OnlineMap(TileProvider provider, byte z)
	{
		super("http://... [" + provider.code + "]");
		datum = "WGS84";
		projection = ProjectionFactory.fromPROJ4Specification("+proj=merc".split(" "));
		projection.setEllipsoid(Ellipsoid.WGS_1984);
	    projection.initialize();
	    
	    tileProvider = provider;
	    tileController = new TileController();

	    title = String.format("%s (%d)", tileProvider.name, z);
		srcZoom = z;
		defZoom = z;
		zoom = 1.;
		dynZoom = 1.;

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
		lastLatitude = 0.;
		mpp = prescaleFactor * projection.getEllipsoid().equatorRadius * Math.PI * 2 * Math.cos(Math.toRadians(lastLatitude)) / Math.pow(2.0, (srcZoom + 8));
	}

	public static void setPrescaleFactor(int factor)
	{
		TILE_WIDTH = ORIG_TILE_WIDTH * factor;
		TILE_HEIGHT = ORIG_TILE_HEIGHT * factor;
		prescaleFactor = 1. / factor;
	}

	@Override
	public synchronized void activate(DisplayMetrics metrics, double zoom) throws IOException, OutOfMemoryError
	{
		displayWidth = metrics.widthPixels;
		displayHeight = metrics.heightPixels;

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

		tileController.setProvider(tileProvider);
		recalculateCache();
		isActive = true;
	}
	
	@Override
	public synchronized void deactivate()
	{
		if (!isActive)
			return;
		isActive = false;
		tileController.interrupt();
		cache.destroy();
		if (savedZoom != 0.)
			zoom = savedZoom;
		savedZoom = 0.;
		cache = null;
	}
	
	public boolean activated()
	{
		return isActive;
	}
	
	public TileProvider getTileProvider()
	{
		return tileProvider;
	}

	@Override
	public boolean coversLatLon(double lat, double lon)
	{
		if (! isActive)
		{
			lastLatitude = lat;
			mpp = prescaleFactor * projection.getEllipsoid().equatorRadius * Math.PI * 2 * Math.cos(Math.toRadians(lastLatitude)) / Math.pow(2.0, (srcZoom + 8));
		}
		return lat < 85.051129 && lat > -85.047336;
	}

	@Override
	public boolean coversScreen(int[] map_xy, int width, int height)
	{
		// TODO Should check North and South edges
		return true;
	}
	
	@Override
	public boolean containsArea(Bounds area)
	{
		// FIXME disabled online maps in adjacent maps
		return false;
//		return area.minLat < 85.051129 && area.maxLat > -85.047336;
	}
	
	@Override
	public boolean drawMap(double[] loc, int[] lookAhead, int width, int height, boolean cropBorder, boolean drawBorder, Canvas c) throws OutOfMemoryError
	{
		int[] map_xy = new int[2];
		getXYByLatLon(loc[0], loc[1], map_xy);
		map_xy[0] -= lookAhead[0];
		map_xy[1] -= lookAhead[1];
		
		float tile_w = (float) (TILE_WIDTH * dynZoom);
		float tile_h = (float) (TILE_HEIGHT * dynZoom);

		int osm_x = (int) (map_xy[0] / tile_w);
		int osm_y = (int) (map_xy[1] / tile_h);

		int tiles_per_x = Math.round(width * 1.f / tile_w / 2 + .5f);
		int tiles_per_y = Math.round(height * 1.f / tile_h / 2 + .5f);

		int c_min = osm_x - tiles_per_x;
		int c_max = osm_x + tiles_per_x + 1;
		
		int r_min = osm_y - tiles_per_y;
		int r_max = osm_y + tiles_per_y + 1;
		
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
		int tw = Math.round(tile_w);
		int th = Math.round(tile_h);
		
		for (int i = r_min; i < r_max; i++)
		{
			for (int j = c_min; j < c_max; j++)
			{
				Bitmap tile = getTile(j, i);

				if (tile != null && ! tile.isRecycled())
				{
					if (tile.getWidth() != tw)
					{
						Bitmap scaled = Bitmap.createScaledBitmap(tile, tw, th, true);
						tile = scaled;
					}
					float tx = w2mx + j * tile_w;
					float ty = h2my + i * tile_h;
					c.drawBitmap(tile, tx, ty, null);
				}
			}
		}
		return result;
	}

	public Bitmap getTile(int x, int y) throws OutOfMemoryError
	{
		Tile tile = tileController.getTile(x, y, srcZoom);
		if (tile.bitmap != null)
		{
			if (dynZoom != 1.0)
			{
		        int sw = (int) (dynZoom * TILE_WIDTH);
		        int sh = (int) (dynZoom * TILE_HEIGHT);
				Bitmap scaled = Bitmap.createScaledBitmap(tile.bitmap, sw, sh, true);
				tile.bitmap = scaled;
			}
		}
		return tile.bitmap;
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
	public Bounds getBounds()
	{
		if (bounds == null)
		{
			bounds = new Bounds();
			bounds.minLat = -85.047336;
			bounds.maxLat = 85.051129;
			bounds.minLon = -180;
			bounds.maxLon = 180;
		}
		return bounds;
	}

	@Override
	public boolean getLatLonByXY(int x, int y, double[] ll)
	{
		int map_x = (int) (x * 1. / dynZoom);
		int map_y = (int) (y * 1. / dynZoom);
		double dx = map_x * 1. / TILE_WIDTH;
		double dy = map_y * 1. / TILE_HEIGHT;
		
		double n = Math.pow(2.0, srcZoom);
		if (tileProvider.ellipsoid)
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

		if (tileProvider.ellipsoid)
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

	public boolean getOsmXYByLatLon(double lat, double lon, int[] xy)
	{
		double n = Math.pow(2.0, srcZoom);

		xy[0] = (int) Math.floor((lon + 180) / 360 * n);
		if (xy[0] == n)
			xy[0] -= 1;
		if (tileProvider.ellipsoid)
		{
			double z = Math.sin(Math.toRadians(lat));
			xy[1] = (int) Math.floor((1 - (atanh(z)-0.0818197*atanh(0.0818197*z)) / Math.PI) / 2 * n);
		}
		else
		{
			xy[1] = (int) Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * n);
		}
		if (xy[1] < 0)
			xy[1] = 0;
		return true;
	}
	
	@Override
	public double getMPP()
	{
		return mpp / dynZoom;
	}

	@Override
	public double getNextZoom()
	{
		if (srcZoom >= tileProvider.maxZoom)
			return 0.0;
		Log.e("ONLINE", "Next zoom: " + Math.pow(2, this.srcZoom + 1 - defZoom));
		return Math.pow(2, this.srcZoom + 1 - defZoom);
	}

	@Override
	public void recalculateCache()
	{
		TileRAMCache oldcache = cache;
		int nx = (int) Math.ceil(displayWidth * 1. / (TILE_WIDTH * dynZoom)) + 2;
		int ny = (int) Math.ceil(displayHeight * 1. / (TILE_HEIGHT * dynZoom)) + 2;
		int cacheSize = nx * ny;
		Log.e("ONLINE", "Cache size: " + cacheSize);
		cache = new TileRAMCache(cacheSize);
		tileController.setCache(cache);
		if (oldcache != null)
		{
			oldcache.destroy();
		}
	}

	@Override
	public double getPrevZoom()
	{
		if (srcZoom <= tileProvider.minZoom)
			return 0.0;
		Log.e("ONLINE", "Prev zoom: " + Math.pow(2, this.srcZoom - 1 - defZoom));
		return Math.pow(2, this.srcZoom - 1 - defZoom);
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
		Log.e("ONLINE", "Zoom: " + z + " diff: " + zDiff);

		srcZoom = (byte) (defZoom + zDiff);
		
		if (srcZoom > tileProvider.maxZoom)
		{
			zDiff -= srcZoom - tileProvider.maxZoom;
			srcZoom = tileProvider.maxZoom;
		}
		if (srcZoom < tileProvider.minZoom)
		{
			zDiff -= srcZoom - tileProvider.minZoom;
			srcZoom = tileProvider.minZoom;
		}

		zoom = z;
		dynZoom = zoom / Math.pow(2, srcZoom - defZoom);
		if (Math.abs(dynZoom - 1) < 0.0078125)
			dynZoom = 1.0;
		Log.e("ONLINE", "z: " + srcZoom + " diff: " + zDiff + " zoom: " + zoom + " dymZoom: " + dynZoom);
		
		recalculateCache();
		
		tileController.reset();
	    title = String.format("%s (%d)", tileProvider.name, srcZoom);
	    
		mpp = prescaleFactor * projection.getEllipsoid().equatorRadius * Math.PI * 2 * Math.cos(Math.toRadians(lastLatitude)) / Math.pow(2.0, (srcZoom + 8));
	}

	public int getScaledWidth()
	{
		return (int) (Math.pow(2.0, srcZoom) * TILE_WIDTH * zoom);
	}

	public int getScaledHeight()
	{
		return (int) (Math.pow(2.0, srcZoom) * TILE_HEIGHT * zoom);
	}

	public List<String> info()
	{
		ArrayList<String> info = new ArrayList<String>();
		
		info.add("title: " + title);
		if (projection != null)
		{
			info.add("projection: " + prjName + " (" + projection.getEPSGCode() + ")");
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
