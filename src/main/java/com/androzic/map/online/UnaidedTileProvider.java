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

import java.util.ArrayList;
import java.util.Locale;

public class UnaidedTileProvider extends TileProvider
{
	protected ArrayList<String> servers = new ArrayList<>();
	protected String secret;
	protected int nextServer = 0;
	//TODO Better initialization?
	protected String locale = Locale.getDefault().toString();

	@Override
	public void activate()
	{
	}

	@Override
	public void deactivate()
	{
	}

	@Nullable
	@Override
	public String getTileUri(int x, int y, byte z)
	{
		String uri = this.uri;

		if (! servers.isEmpty())
		{
			if (servers.size() <= nextServer)
				nextServer = 0;
			uri = uri.replace("{$s}", servers.get(nextServer));
			nextServer++;
		}
		if (inverseY)
			y = (int) (Math.pow(2, z) - 1 - y);
		uri = uri.replace("{$l}", locale);
		uri = uri.replace("{$z}", String.valueOf(z));
		uri = uri.replace("{$x}", String.valueOf(x));
		uri = uri.replace("{$y}", String.valueOf(y));
		if (uri.contains("{$q}"))
			uri = uri.replace("{$q}", encodeQuadTree(z, x, y));
		if (uri.contains("{$g}") && secret != null)
		{
			int stringlen = (3 * x + y) & 7;
			uri = uri.replace("{$g}", secret.substring(0, stringlen));
		}

		return uri;
	}

	private static final char[] NUM_CHAR = { '0', '1', '2', '3' };

	/**
	 * See: http://msdn.microsoft.com/en-us/library/bb259689.aspx
	 * @param zoom tile zoom
	 * @param tilex tile X
	 * @param tiley tile Y
	 * @return quadtree encoded tile number
	 *
	 */
	private static String encodeQuadTree(int zoom, int tilex, int tiley)
	{
		char[] tileNum = new char[zoom];
		for (int i = zoom - 1; i >= 0; i--)
		{
			// Binary encoding using ones for tilex and twos for tiley. if a bit
			// is set in tilex and tiley we get a three.
			int num = (tilex % 2) | ((tiley % 2) << 1);
			tileNum[i] = NUM_CHAR[num];
			tilex >>= 1;
			tiley >>= 1;
		}
		return new String(tileNum);
	}
}
