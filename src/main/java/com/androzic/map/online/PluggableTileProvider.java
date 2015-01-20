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

import android.content.ContentProviderClient;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import com.androzic.BaseApplication;

public class PluggableTileProvider extends TileProvider
{
	public static final String[] TILE_COLUMNS = new String[] {"TILE"};

	private ContentProviderClient providerClient;

	@Override
	public void activate()
	{
		BaseApplication application = BaseApplication.getApplication();
		providerClient = application.getContentResolver().acquireContentProviderClient(Uri.parse(uri));
	}

	@Override
	public void deactivate()
	{
		if (providerClient != null)
		{
			providerClient.release();
			providerClient = null;
		}
	}

	@Nullable
	@Override
	public String getTileUri(int x, int y, byte z)
	{
		if (providerClient == null)
			return null;
		Uri contentUri = Uri.parse(uri + "/" + z + "/" + x + "/" + y);
		try
		{
			Cursor cursor = providerClient.query(contentUri, TILE_COLUMNS, null, null, null);
			cursor.moveToFirst();
			return cursor.getString(0);
		}
		catch (RemoteException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
