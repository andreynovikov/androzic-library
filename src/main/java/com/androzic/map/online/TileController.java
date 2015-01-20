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

import java.util.Hashtable;
import java.util.LinkedList;

import com.androzic.map.Tile;
import com.androzic.map.TileRAMCache;

public class TileController extends Thread
{
	final LinkedList<Tile> pendingList = new LinkedList<>();
	final Hashtable<Long, Tile> tileMap = new Hashtable<>();
	Thread[] threads;
	private TileProvider provider;
	private TileRAMCache cache;

	public TileController(TileProvider provider)
	{
		this.provider = provider;
		threads = new Thread[provider.threads];
		for (int i = 0; i < threads.length; i++)
		{
			threads[i] = new Thread(this);
			threads[i].start();
		}
	}

	public void run()
	{
		while (!this.isInterrupted())
		{
			try
			{
				Tile t;
				synchronized (pendingList)
				{
					t = pendingList.poll();
				}
				if (t == null)
				{
					synchronized (this)
					{
						wait();
					}
					continue;
				}
				long key = t.getKey();
				tileMap.remove(key);
				TileFactory.downloadTile(provider, t);
				if (t.bitmap != null)
				{
					TileFactory.saveTile(provider, t);
					cache.put(key, t);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * Interrupts all the Threads
	 */
	public void interrupt()
	{
		for (Thread thread : threads)
			thread.interrupt();
	}

	/**
	 * 
	 * @param tx
	 *            The X position of the Tile to draw
	 * @param ty
	 *            The Y position of the Tile to draw
	 * @param tz
	 *            The Zoom value of the Tile to draw
	 * @return The recovered Tile
	 */
	public Tile getTile(int tx, int ty, byte tz)
	{
		long key = Tile.getKey(tx, ty, tz);
		Tile t = cache.get(key);
		if (t == null)
		{
			t = tileMap.get(key);
		}
		if (t == null)
		{
			t = new Tile(tx, ty, tz);
			TileFactory.loadTile(provider, t);
			if (t.expired)
				queueForDownload(key, t);
			if (t.bitmap == null)
			{
				TileFactory.generateTile(provider, cache, t);
				if (t.bitmap != null)
					cache.put(key, t);
				queueForDownload(key, t);
			}
			else
			{
				cache.put(key, t);
			}
		}
		return t;
	}

	private void queueForDownload(long key, Tile tile)
	{
		tileMap.put(key, tile);
		synchronized (pendingList)
		{
			pendingList.add(tile);
		}
		synchronized (this)
		{
			notifyAll();
		}
	}

	/**
	 * Reset tile download queue
	 */
	public void reset()
	{
		tileMap.clear();
		synchronized (pendingList)
		{
			pendingList.clear();
		}
	}

	public void setCache(TileRAMCache cache)
	{
		this.cache = cache;
	}
}