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
import android.util.DisplayMetrics;

import com.androzic.data.Bounds;
import com.androzic.ui.Viewport;
import com.jhlabs.map.proj.Projection;
import com.jhlabs.map.proj.ProjectionException;

import java.io.Serializable;
import java.util.List;

public abstract class BaseMap implements Serializable
{
	private static final long serialVersionUID = 1L;

	public int id;
	public String path;
	public String title;

	public String datum;

	protected Projection projection;
	protected MapPoint[] cornerMarkers;
	protected double mpp;

	protected boolean isActive = false;

	protected transient double zoom = 1.;
	protected transient double savedZoom = 0.;

	protected transient Bounds bounds;
	protected transient Path mapClipPath;
	protected transient Paint borderPaint;
	protected transient OnMapTileStateChangeListener listener;
	protected transient TileRAMCache cache;

	protected transient int displayWidth;
	protected transient int displayHeight;

	public transient Throwable loadError;

	public BaseMap(String path)
	{
		this.path = path;
		this.id = path.hashCode();
	}

	public synchronized void activate(OnMapTileStateChangeListener listener, DisplayMetrics metrics, double zoom) throws Throwable
	{
		this.listener = listener;
		displayWidth = metrics.widthPixels;
		displayHeight = metrics.heightPixels;

		borderPaint = new Paint();
		borderPaint.setAntiAlias(true);
		borderPaint.setStrokeWidth(3);
		borderPaint.setColor(Color.RED);
		borderPaint.setAlpha(128);
		borderPaint.setStyle(Paint.Style.STROKE);

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

		isActive = true;
	}

	public synchronized void deactivate()
	{
		isActive = false;

		if (savedZoom != 0.)
			zoom = savedZoom;
		savedZoom = 0.;

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
	public abstract double getMPP();

	public final double getAbsoluteMPP()
	{
		return mpp;
	}

	public abstract double getNextZoom();
	public abstract double getPrevZoom();
	public abstract double getZoom();
	public abstract void setZoom(double z);

	public final void zoomBy(double factor)
	{
		setZoom(zoom * factor);
	}

	public final void setTemporaryZoom(double zoom)
	{
		if (savedZoom == 0.)
			savedZoom = this.zoom;
		setZoom(zoom);
	}

	public abstract boolean drawMap(Viewport viewport, boolean cropBorder, boolean drawBorder, Canvas c) throws OutOfMemoryError;

	public abstract void recalculateCache();

	public abstract List<String> info();

	@Override
	public int hashCode()
	{
		return id;
	}
}
