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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import com.androzic.data.Bounds;
import com.androzic.ui.Viewport;
import com.jhlabs.map.proj.Projection;
import com.jhlabs.map.proj.ProjectionException;

import java.io.Serializable;
import java.util.List;

public abstract class BaseMap implements Serializable
{
	private static final long serialVersionUID = 2L;

	public int id;
	public String path;
	public String title;

	public String datum;

	public static transient int viewportWidth;
	public static transient int viewportHeight;

	protected Projection projection;
	protected MapPoint[] cornerMarkers;
	protected double mpp;

	protected boolean isActive = false;
	protected boolean isCurrent = false;

	protected double zoom = 1.;

	protected transient Bounds bounds;
	protected transient Path mapClipPath;
	protected transient Paint borderPaint;
	protected transient OnMapTileStateChangeListener listener;
	protected transient TileRAMCache cache;
	protected transient int width;
	protected transient int height;

	public transient Throwable loadError;

	protected BaseMap()
	{
	}

	public BaseMap(String path)
	{
		this.path = path;
		this.id = path.hashCode();
	}

	/**
	 * Called after map object is created.
	 */
	public abstract void initialize();

	/**
	 * Called before map removal.
	 */
	public abstract void destroy();

	public synchronized void activate(OnMapTileStateChangeListener listener, double mpp, boolean current) throws Throwable
	{
		this.listener = listener;
		this.isCurrent = current;

		borderPaint = new Paint();
		borderPaint.setAntiAlias(true);
		borderPaint.setStrokeWidth(3);
		borderPaint.setColor(Color.RED);
		borderPaint.setAlpha(128);
		borderPaint.setStyle(Paint.Style.STROKE);

		mapClipPath = new Path();

		// We do not use zoomTo() to overpass same mpp check
		setZoom(getAbsoluteMPP() / mpp);

		isActive = true;
	}

	public synchronized void deactivate()
	{
		isActive = false;

		if (cache != null)
			cache.destroy();
		cache = null;

		listener = null;
		borderPaint = null;
		mapClipPath = null;
	}

	public boolean activated()
	{
		return isActive;
	}

	public synchronized void recalculateCache()
	{
	}

	public void setCornersAmount(int num)
	{
		cornerMarkers = new MapPoint[num];
		for (int i = 0; i < num; i++)
		{
			cornerMarkers[i] = new MapPoint();
		}
	}

	public Bounds getBounds()
	{
		if (bounds == null)
		{
			bounds = new Bounds();
			for (MapPoint corner : cornerMarkers)
				bounds.extend(corner.lat, corner.lon);
		}
		return bounds;
	}

	/**
	 * Checks if map covers given coordinates
	 * @param lat latitude in degrees
	 * @param lon longitude in degrees
	 * @return true if coordinates are inside map
	 */
	public boolean coversLatLon(double lat, double lon)
	{
		//  Note that division by zero is avoided because the division is protected
		//  by the "if" clause which surrounds it.

		int j = cornerMarkers.length - 1;
		int odd = 0;

		for (int i=0; i < cornerMarkers.length; i++)
		{
			if (cornerMarkers[i].lon < lon && cornerMarkers[j].lon >= lon || cornerMarkers[j].lon < lon && cornerMarkers[i].lon >= lon)
			{
				if (cornerMarkers[i].lat + (lon - cornerMarkers[i].lon) / (cornerMarkers[j].lon - cornerMarkers[i].lon) * (cornerMarkers[j].lat - cornerMarkers[i].lat) < lat)
				{
					odd++;
				}
			}
			j=i;
		}

		return odd % 2 == 1;
	}

	/**
	 * Checks if map covers given coordinates
	 * @param lat latitude in degrees
	 * @param lon longitude in degrees
	 * @return true if coordinates are inside map
	 */
	public boolean coversLatLonByXY(double lat, double lon)
	{
		int[] xy = new int[2];
		boolean inside;
		try
		{
			inside = getXYByLatLon(lat, lon, xy);
		}
		catch (ProjectionException e)
		{
			return false;
		}

		// check corners
		if (inside)
		{
			// rescale to original size
			xy[0] = (int) (xy[0] / zoom);
			xy[1] = (int) (xy[1] / zoom);

			//  Note that division by zero is avoided because the division is protected
			//  by the "if" clause which surrounds it.

			int j = cornerMarkers.length - 1;
			int odd = 0;

			for (int i=0; i < cornerMarkers.length; i++)
			{
				if (cornerMarkers[i].y < xy[1] && cornerMarkers[j].y >= xy[1] || cornerMarkers[j].y < xy[1] && cornerMarkers[i].y >= xy[1])
				{
					if (cornerMarkers[i].x + (xy[1] - cornerMarkers[i].y) * 1. / (cornerMarkers[j].y - cornerMarkers[i].y) * (cornerMarkers[j].x - cornerMarkers[i].x) < xy[0])
					{
						odd++;
					}
				}
				j=i;
			}

			inside = odd % 2 == 1;
		}

		return inside;
	}

	public boolean containsArea(Bounds area)
	{
		Bounds b = getBounds();
		return b.intersects(area);
	}

	public abstract int getScaledWidth();
	public abstract int getScaledHeight();

	public abstract boolean getLatLonByXY(int x, int y, double[] ll);
	public abstract boolean getXYByLatLon(double lat, double lon, int[] xy);
	public abstract void getMapCenter(double[] center);

	/**
	 * Returns current mpp with respect to zoom
	 */
	public abstract double getMPP();

	/**
	 * Returns default (maximum) mpp
	 */
	public double getAbsoluteMPP()
	{
		return mpp;
	}

	/**
	 * Returns scale ratio with respect to reference map
	 * @param refMpp reference map MPP
	 * @return ratio, where 1d means equality, &gt;1d means larger (better) scale, &lt;1d means less scale
	 */
	public abstract double getCoveringRatio(double refMpp);

	public abstract double getNextZoom();
	public abstract double getPrevZoom();
	public abstract double getZoom();
	public abstract void setZoom(double z);

	public final void zoomTo(double mpp)
	{
		if (mpp == getMPP())
			return;
		setZoom(getAbsoluteMPP() / mpp);
	}

	public final void zoomBy(double factor)
	{
		setZoom(zoom * factor);
	}

	public abstract boolean drawMap(Viewport viewport, boolean cropBorder, boolean drawBorder, Canvas c) throws OutOfMemoryError;

	public abstract int getPriority();
	public abstract List<String> info();

	@Override
	public int hashCode()
	{
		return id;
	}
}
