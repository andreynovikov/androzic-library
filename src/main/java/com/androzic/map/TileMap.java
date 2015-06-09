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
 *
 * Androzic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Androzic. If not, see <http://www.gnu.org/licenses/>.
 */

package com.androzic.map;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;

import com.androzic.Log;
import com.androzic.ui.Viewport;
import com.jhlabs.map.Ellipsoid;
import com.jhlabs.map.proj.ProjectionFactory;

public abstract class TileMap extends BaseMap
{
	private static final long serialVersionUID = 2L;

	public static final int TILE_SIZE = 256;

	protected byte srcZoom;
	protected byte defZoom;
	protected double dynZoom;

	public String name;
	public byte minZoom = 0;
	public byte maxZoom = 18;
	public boolean ellipsoid = false;

	protected int tileSize = 256;
	private double prescaleFactor = 1.;

	protected transient double lastLatitude;
	private transient double defMPP;

	protected TileMap()
	{
	}

	public TileMap(String path)
	{
		super(path);

		datum = "WGS84";
		projection = ProjectionFactory.fromPROJ4Specification("+proj=merc".split(" "));
		projection.setEllipsoid(Ellipsoid.WGS_1984);
		projection.initialize();

		zoom = 1.;
		dynZoom = 1.;
		srcZoom = 14;
		defZoom = 14;

		lastLatitude = 0.;
		recalculateMPP();
	}

	protected void initializeZooms(byte min, byte max, byte def)
	{
		minZoom = min;
		maxZoom = max;
		srcZoom = def;
		defZoom = def;
		recalculateMPP();
	}

	public void setPrescaleFactor(int factor)
	{
		tileSize = TILE_SIZE * factor;
		prescaleFactor = 1. / factor;
	}

	@Override
	public boolean coversLatLon(double lat, double lon)
	{
		if (! isActive)
		{
			lastLatitude = lat;
			recalculateMPP();
		}
		return super.coversLatLon(lat, lon);
	}

	@Override
	public int getScaledWidth()
	{
		return (int) (Math.pow(2.0, srcZoom) * tileSize * zoom);
	}

	@Override
	public int getScaledHeight()
	{
		return (int) (Math.pow(2.0, srcZoom) * tileSize * zoom);
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
		double dx = map_x * 1. / tileSize;
		double dy = map_y * 1. / tileSize;

		double n = Math.pow(2.0, srcZoom);
		if (ellipsoid)
		{
			ll[0] = (map_y - tileSize * n / 2) / -(tileSize * n / (2 * Math.PI));
			ll[0] = (2 * Math.atan(Math.exp(ll[0])) - Math.PI / 2) * 180 / Math.PI;

			double Zu = Math.toRadians(ll[0]);
			double Zum1 = Zu + 1;
			double yy = (map_y - tileSize * n / 2);
			int i = 100000;
			while ((Math.abs(Zum1 - Zu) > 0.0000001) && (i != 0))
			{
				i--;
				Zum1 = Zu;
				Zu = Math.asin(1 - ((1 + Math.sin(Zum1)) * Math.pow(1 - 0.0818197 * Math.sin(Zum1), 0.0818197))
						/ (Math.exp((2 * yy) / -(tileSize * n / (2 * Math.PI))) * Math.pow(1 + 0.0818197 * Math.sin(Zum1), 0.0818197)));
			}
			ll[0] = Math.toDegrees(Zu);
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

		xy[0] = (int) Math.floor((lon + 180.0) / 360.0 * n * tileSize * dynZoom);

		if (ellipsoid)
		{
			double z = Math.sin(Math.toRadians(lat));
			xy[1] = (int) Math.floor((1 - (atanh(z) - 0.0818197 * atanh(0.0818197 * z)) / Math.PI) / 2 * n * tileSize * dynZoom);
		}
		else
		{
			xy[1] = (int) Math.floor((1 - (Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI)) / 2 * n * tileSize * dynZoom);
		}
		return true;
	}

	public void getTileXYByLatLon(double lat, double lon, int[] xy)
	{
		double n = Math.pow(2.0, srcZoom);

		xy[0] = (int) Math.floor((lon + 180) / 360 * n) ;

		if (ellipsoid)
		{
			double z = Math.sin(Math.toRadians(lat));
			xy[1] = (int) Math.floor((1 - (atanh(z)-0.0818197*atanh(0.0818197*z)) / Math.PI) / 2 * n);
		}
		else
		{
			xy[1] = (int) Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * n);
		}

		if (xy[0] < 0)
			xy[0] = 0;
		if (xy[0] >= n)
			xy[0] = (int) n - 1;
		if (xy[1] < 0)
			xy[1] = 0;
		if (xy[1] >= n)
			xy[1] = (int) n - 1;
	}

	@Override
	public void getMapCenter(double[] center)
	{
		int x = (int) ((cornerMarkers[2].x + cornerMarkers[0].x) / 2 * zoom);
		int y = (int) ((cornerMarkers[2].y + cornerMarkers[0].y) / 2 * zoom);
		getLatLonByXY(x, y, center);
	}

	@Override
	public double getMPP()
	{
		return mpp / dynZoom;
	}

	@Override
	public double getAbsoluteMPP()
	{
		return defMPP;
	}

	@Override
	public double getCoveringRatio(double refMpp)
	{
		double zMpp;
		for (int z = maxZoom; z > minZoom; z--)
		{
			zMpp = prescaleFactor * projection.getEllipsoid().equatorRadius * Math.PI * 2 * Math.cos(Math.toRadians(lastLatitude)) / Math.pow(2.0, (z + 8));
			double ratio = refMpp / zMpp;
			if (ratio <= 5d)
				return ratio;
		}
		zMpp = prescaleFactor * projection.getEllipsoid().equatorRadius * Math.PI * 2 * Math.cos(Math.toRadians(lastLatitude)) / Math.pow(2.0, (minZoom + 8));
		return refMpp / zMpp;
	}

	@Override
	public double getNextZoom()
	{
		int z = defZoom + (int) (Math.log(zoom) / Math.log(2));
		if (z - maxZoom > 0)
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
		if (z == 0 || z - minZoom < -1)
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
		int zDiff = (int) Math.round(Math.log(z) / Math.log(2));
		Log.e("TileMap", "Zoom: " + z + " diff: " + zDiff);

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
		Log.e("TileMap", "z: " + srcZoom + " diff: " + zDiff + " zoom: " + zoom + " dymZoom: " + dynZoom);

		recalculateCache();
		updateTitle();
		recalculateMPP();

		mapClipPath.rewind();
		mapClipPath.setLastPoint((float) (cornerMarkers[0].x * zoom), (float) (cornerMarkers[0].y * zoom));
		for (int i = 1; i < cornerMarkers.length; i++)
			mapClipPath.lineTo((float) (cornerMarkers[i].x * zoom), (float) (cornerMarkers[i].y * zoom));
		mapClipPath.close();
	}

	protected void updateTitle()
	{
		title = String.format("%s (%d)", name, srcZoom);
	}

	protected void recalculateMPP()
	{
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
		mpp = prescaleFactor * projection.getEllipsoid().equatorRadius * Math.PI * 2 * Math.cos(Math.toRadians(lastLatitude)) / Math.pow(2.0, (srcZoom + 8));
		defMPP = prescaleFactor * projection.getEllipsoid().equatorRadius * Math.PI * 2 * Math.cos(Math.toRadians(lastLatitude)) / Math.pow(2.0, (defZoom + 8));
	}

	@Override
	public boolean drawMap(Viewport viewport, boolean cropBorder, boolean drawBorder, Canvas c) throws OutOfMemoryError
	{
		if (!isActive)
			return false;

		lastLatitude = viewport.mapCenter[0];
		recalculateMPP();

		int[] map_xy = new int[2];
		getXYByLatLon(viewport.mapCenter[0], viewport.mapCenter[1], map_xy);
		map_xy[0] -= viewport.lookAheadXY[0];
		map_xy[1] -= viewport.lookAheadXY[1];

		Path clipPath = new Path();

		if (cropBorder || drawBorder)
			mapClipPath.offset(-map_xy[0] + viewport.canvasWidth / 2, -map_xy[1] + viewport.canvasHeight / 2, clipPath);

		float tile_wh = (float) (tileSize * dynZoom);

		int osm_x = (int) (map_xy[0] / tile_wh);
		int osm_y = (int) (map_xy[1] / tile_wh);

		int tiles_per_x = Math.round(viewport.canvasWidth * 1.f / tile_wh / 2 + .5f);
		int tiles_per_y = Math.round(viewport.canvasHeight * 1.f / tile_wh / 2 + .5f);

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

		float w2mx = viewport.canvasWidth / 2 - map_xy[0];
		float h2my = viewport.canvasHeight / 2 - map_xy[1];
		int twh = Math.round(tile_wh);

		int i = osm_y, j = osm_x, dx = 0, dy = -1;
		int t = Math.max(c_max - c_min + 1, r_max - r_min + 1);
		int maxI = t*t;

		for (int k = 0; k < maxI; k++)
		{
			if (c_min <= j && j <= c_max && r_min <= i && i <= r_max)
			{
				Bitmap tile = getTile(j, i);
				if (tile != null && ! tile.isRecycled())
				{
					if (tile.getWidth() != twh)
						tile = Bitmap.createScaledBitmap(tile, twh, twh, true);
					float tx = w2mx + j * tile_wh;
					float ty = h2my + i * tile_wh;
					c.drawBitmap(tile, tx, ty, null);
				}
				else
				{
					result = false;
				}
			}

			int x = j - osm_x, y = i - osm_y;
			if( (x == y) || ((x < 0) && (x == -y)) || ((x > 0) && (x == 1-y)))
			{
				t = dx;
				dx = -dy;
				dy = t;
			}
			j += dx;
			i += dy;
		}

		if (drawBorder && borderPaint != null)
			c.drawPath(clipPath, borderPaint);

		return result;
	}

	@Override
	public synchronized void recalculateCache()
	{
		TileRAMCache oldCache = cache;
		int nx = (int) Math.ceil(viewportWidth * 1. / (tileSize * dynZoom)) + 2;
		int ny = (int) Math.ceil(viewportHeight * 1. / (tileSize * dynZoom)) + 2;
		int cacheSize = nx * ny;
		Log.e("TileMap", "Cache size: " + cacheSize);
		cache = new TileRAMCache(cacheSize);
		if (oldCache != null)
			oldCache.destroy();
	}

	protected abstract Bitmap getTile(int x, int y) throws OutOfMemoryError;
}
