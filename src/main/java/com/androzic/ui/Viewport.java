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

package com.androzic.ui;

import android.graphics.Rect;

import com.androzic.data.Bounds;

public class Viewport
{
	/**
	 * Map center geodetic coordinates
	 */
	public double[] mapCenter;
	/**
	 * Map center linear coordinates
	 */
	public int[] mapCenterXY;
	/**
	 * Map rotation angle in degrees
	 */
	public float mapHeading;
	/**
	 * Current GPS location, if location is not available, coordinates are set to Double.NaN
	 */
	public double[] location;
	/**
	 * Current GPS location linear coordinates
	 */
	public int[] locationXY;
	/**
	 * Physical width of a map view
	 */
	public int width;
	/**
	 * Physical height of a map view
	 */
	public int height;
	/**
	 * Logical width of a map view
	 */
	public int canvasWidth;
	/**
	 * Logical height of a map view
	 */
	public int canvasHeight;
	/**
	 * The area of a map view that is not covered by UI elements
	 */
	public Rect viewArea;
	/**
	 * Currently visible map area
	 */
	public Bounds mapArea;

	public int[] lookAheadXY;

	public float bearing;
	public float speed = 0f;

	public Viewport()
	{
		mapCenter = new double[2];
		mapCenterXY = new int[2];
		mapHeading = 0f;
		location = new double[] {Double.NaN, Double.NaN};
		locationXY = new int[2];

		width = 0;
		height = 0;
		canvasWidth = 0;
		canvasHeight = 0;

		viewArea = new Rect();
		mapArea = new Bounds();

		bearing = 0f;
		speed = 0f;

		lookAheadXY = new int[] { 0, 0 };
	}

	public Viewport copy()
	{
		Viewport copy = new Viewport();
		copy.mapCenter[0] = mapCenter[0];
		copy.mapCenter[1] = mapCenter[1];
		copy.mapCenterXY[0] = mapCenterXY[0];
		copy.mapCenterXY[1] = mapCenterXY[1];
		copy.mapHeading = mapHeading;
		copy.location[0] = location[0];
		copy.location[1] = location[1];
		copy.locationXY[0] = locationXY[0];
		copy.locationXY[1] = locationXY[1];
		copy.width = width;
		copy.height = height;
		copy.canvasWidth = canvasWidth;
		copy.canvasHeight = canvasHeight;
		copy.viewArea = new Rect(viewArea);
		copy.mapArea = new Bounds(mapArea);
		copy.bearing = bearing;
		copy.speed = speed;
		copy.lookAheadXY[0] = lookAheadXY[0];
		copy.lookAheadXY[1] = lookAheadXY[1];
		return copy;
	}
}

