/*
 * Androzic - android navigation client that uses OziExplorer maps (ozf2, ozfx3).
 * Copyright (C) 2010-2014 Andrey Novikov <http://andreynovikov.info/>
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

package com.androzic.data;

public class Bounds
{
	public double minLat = Double.MAX_VALUE;
	public double maxLat = Double.MIN_VALUE;
	public double minLon = Double.MAX_VALUE;
	public double maxLon = Double.MIN_VALUE;

	public Bounds()
	{
	}
	
	public Bounds(Bounds bounds)
	{
		minLat = bounds.minLat;
		maxLat = bounds.maxLat;
		minLon = bounds.minLon;
		maxLon = bounds.maxLon;
	}

	public void extend(double latitude, double longitude)
	{
		if (latitude < minLat)
			minLat = latitude;
		if (latitude > maxLat)
			maxLat = latitude;
		// FIXME think how to wrap 180 parallel
		if (longitude < minLon)
			minLon = longitude;
		if (longitude > maxLon)
			maxLon = longitude;
	}

	public void fix()
	{
		if (minLat < -90d)
			minLat = -90d;
		if (maxLat > 90d)
			maxLat = 90d;
		if (minLon < -180d)
			minLon = -180d;
		if (maxLon > 180d)
			maxLon = 180d;
	}

	public boolean intersects(Bounds area)
	{
        return intersects(this, area);
	}

	public static boolean intersects(Bounds a, Bounds b)
	{
		//FIXME Should wrap 180 parallel
		return a.minLon < b.maxLon && b.minLon < a.maxLon
                && a.minLat < b.maxLat && b.minLat < a.maxLat;
	}
	
	public String toString()
	{
		return "[" + maxLat + "," + minLon + "," + minLat + "," + maxLon + "]";
	}
}
