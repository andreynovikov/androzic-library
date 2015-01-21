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

import android.support.annotation.Nullable;

import com.androzic.map.OnMapTileStateChangeListener;

public abstract class TileProvider
{
	public static final String TILE_TYPE = "vnd.android.cursor.item/vnd.com.androzic.map.online.provider.tile";
	/**
	 * Tile provider uri
	 */
	public String uri;
	/**
	 * Human readable name
	 */
	public String name;
	/**
	 * Machine friendly code
	 */
	public String code;
	/**
	 * License string (can be HTML containing links)
	 */
	public String license;
	public byte minZoom = 0;
	public byte maxZoom = 18;
	public boolean inverseY = false;
	public boolean ellipsoid = false;
	public OnlineMap instance = null;
	public OnMapTileStateChangeListener listener = null;
	public int tileSize = 25000;
	/**
	 * Tile TTL in milliseconds
	 */
	public int tileExpiration = 0;
	public int threads = 4;

	public abstract void activate();

	public abstract void deactivate();

	@Nullable
	public abstract String getTileUri(int x, int y, byte z);
}
