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

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;

import com.androzic.util.CSV;

import java.util.ArrayList;
import java.util.List;

public class TileProviderFactory
{
	public static TileProvider fromString(String s)
	{
		UnaidedTileProvider provider = new UnaidedTileProvider();
		String[] fields = CSV.parseLine(s);
		if (fields.length < 6)
			return null;
		if ("".equals(fields[0]) || "".equals(fields[1]) || "".equals(fields[5]))
			return null;
		provider.name = fields[0];
		provider.code = fields[1];
		provider.uri = fields[5];
		provider.uri = provider.uri.replace("{comma}", ",");
		try
		{
			provider.minZoom = (byte) Integer.parseInt(fields[2]);
			provider.maxZoom = (byte) Integer.parseInt(fields[3]);
			if (! "".equals(fields[4]))
				provider.tileSize = Integer.parseInt(fields[4]);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
		if (fields.length > 6 && ! "".equals(fields[6]))
			provider.servers.add(fields[6]);
		if (fields.length > 7 && ! "".equals(fields[7]))
			provider.servers.add(fields[7]);
		if (fields.length > 8 && ! "".equals(fields[8]))
			provider.servers.add(fields[8]);
		if (fields.length > 9 && ! "".equals(fields[9]))
			provider.servers.add(fields[9]);
		provider.inverseY = fields.length > 10 && "yinverse".equals(fields[10]);
		provider.ellipsoid = fields.length > 10 && "ellipsoid".equals(fields[10]);
		if (fields.length > 11 && ! "".equals(fields[11]))
			provider.secret = fields[11];

		return provider;
	}

	public static List<TileProvider> fromPlugin(PackageManager packageManager, ResolveInfo provider)
	{
		List<TileProvider> providers = new ArrayList<>();

		int id;
		String[] maps = null;
		try
		{
			Resources resources = packageManager.getResourcesForApplication(provider.activityInfo.applicationInfo);
			id = resources.getIdentifier("maps", "array", provider.activityInfo.packageName);
			if (id != 0)
				maps = resources.getStringArray(id);

			if (maps == null)
				return providers;

			for (String map : maps)
			{
				String name = null;
				String uri = null;
				id = resources.getIdentifier(map + "_name", "string", provider.activityInfo.packageName);
				if (id != 0)
					name = resources.getString(id);
				id = resources.getIdentifier(map + "_uri", "string", provider.activityInfo.packageName);
				if (id != 0)
					uri = resources.getString(id);
				if (name == null || uri == null)
					continue;
				PluggableTileProvider tileProvider = new PluggableTileProvider();
				tileProvider.name = name;
				tileProvider.code = map;
				tileProvider.uri = uri;

				id = resources.getIdentifier(map + "_license", "string", provider.activityInfo.packageName);
				if (id != 0)
					tileProvider.license = resources.getString(id);
				id = resources.getIdentifier(map + "_threads", "integer", provider.activityInfo.packageName);
				if (id != 0)
					tileProvider.threads = resources.getInteger(id);
				id = resources.getIdentifier(map + "_minzoom", "integer", provider.activityInfo.packageName);
				if (id != 0)
					tileProvider.minZoom = (byte) resources.getInteger(id);
				id = resources.getIdentifier(map + "_maxzoom", "integer", provider.activityInfo.packageName);
				if (id != 0)
					tileProvider.maxZoom = (byte) resources.getInteger(id);

				providers.add(tileProvider);
			}
		}
		catch (Resources.NotFoundException | PackageManager.NameNotFoundException e)
		{
			e.printStackTrace();
		}
		return providers;
	}
}
