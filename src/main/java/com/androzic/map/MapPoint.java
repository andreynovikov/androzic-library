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

package com.androzic.map;

import java.io.Serializable;

public class MapPoint implements Serializable
{
	private static final long serialVersionUID = 2L;

	public int x;
	public int y;

	public double lat;
	public double lon;

	public int zone;
	public double n;
	public double e;
	public int hemisphere;

	public MapPoint()
	{
	}

	public MapPoint(MapPoint mp)
	{
		this.x = mp.x;
		this.y = mp.y;
		this.zone = mp.zone;
		this.n = mp.n;
		this.e = mp.e;
		this.hemisphere = mp.hemisphere;
		this.lat = mp.lat;
		this.lon = mp.lon;
	}
}
