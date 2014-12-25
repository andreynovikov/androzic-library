/*
 * Androzic - android navigation client that uses OziExplorer maps (ozf2, ozfx3).
 * Copyright (C) 2010-2013 Andrey Novikov <http://andreynovikov.info/>
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

package com.androzic.navigation;

import android.app.Service;

public abstract class BaseNavigationService extends Service
{
	/**
	 * Action to initiate navigation to map object registered by Androzic (by id),
	 * which allows to navigate to moving object. In this mode navigation
	 * is not restored if application is restarted.
	 */
	public static final String NAVIGATE_MAPOBJECT_WITH_ID = "com.androzic.navigateMapObjectWithId";
	/**
	 * Action to initiate navigation to map object. Navigation is restored if
	 * application is restarted.
	 */
	public static final String NAVIGATE_MAPOBJECT = "com.androzic.navigateMapObject";
	/**
	 * Action to initiate navigation via route. Navigation is restored if
	 * application is restarted.
	 */
	public static final String NAVIGATE_ROUTE = "com.androzic.navigateRoute";

	/**
	 * Map object id as returned by Androzic. Used with NAVIGATE_MAPOBJECT_WITH_ID action. Type: long
	 */
	public static final String EXTRA_ID = "id";
	/**
	 * Map object name. Type: String
	 */
	public static final String EXTRA_NAME = "name";
	/**
	 * Map object latitude. Type: double
	 */
	public static final String EXTRA_LATITUDE = "latitude";
	/**
	 * Map object longitude. Type: double
	 */
	public static final String EXTRA_LONGITUDE = "longitude";
	/**
	 * Map object proximity. Type: int
	 */
	public static final String EXTRA_PROXIMITY = "proximity";
	/**
	 * Route index as returned by Androzic. Type: int
	 */
	public static final String EXTRA_ROUTE_INDEX = "index";
	/**
	 * Route direction: DIRECTION_FORWARD or DIRECTION_REVERSE.
	 */
	public static final String EXTRA_ROUTE_DIRECTION = "direction";
	/**
	 * Route start waypoint index. Zero based, optional. Type: int
	 */
	public static final String EXTRA_ROUTE_START = "start";

	public static final String BROADCAST_NAVIGATION_STATUS = "com.androzic.navigationStatusChanged";
	public static final String BROADCAST_NAVIGATION_STATE = "com.androzic.navigationStateChanged";

	public static final int STATE_STARTED = 1;
	public static final int STATE_NEXTWPT = 2;
	public static final int STATE_REACHED = 3;
	public static final int STATE_STOPED = 4;

	public static final int DIRECTION_FORWARD = 1;
	public static final int DIRECTION_REVERSE = -1;
}
