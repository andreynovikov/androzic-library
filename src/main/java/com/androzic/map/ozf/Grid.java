/*
 * Androzic - android navigation client that uses OziExplorer maps (ozf2, ozfx3).
 * Copyright (C) 2010-2014  Andrey Novikov <http://andreynovikov.info/>
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

package com.androzic.map.ozf;

import java.io.Serializable;

public class Grid implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	public boolean enabled;
	public double spacing;
	public boolean autoscale;
	public int color1;
	public int color2;
	public int color3;
	public double labelSpacing;
	public int labelForeground;
	public int labelBackground;
	public int labelSize;
	public boolean labelShowEverywhere;
	public int maxMPP = 0;
}
