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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;

import com.androzic.Log;
import com.androzic.map.BaseMap;
import com.androzic.map.MapPoint;
import com.androzic.map.OnMapTileStateChangeListener;
import com.androzic.map.TileRAMCache;
import com.androzic.ui.Viewport;
import com.jhlabs.Point2D;

public class OzfMap extends BaseMap
{
	private static final long serialVersionUID = 11L;

	private static final double[] zoomLevelsSupported =
	{
		// zoom must give integer if multiplied by 50 - it is used as a tile key
		0.02,
		0.06,
		0.10,
		0.25,
		0.50,
		0.75,
		1.00,
		1.25,
		1.50,
		1.75,
		2.00,
		2.50,
		3.00,
		4.00,
		5.00
	};

	public String imagePath;
	public String origDatum;
	public int width;
	public int height;
	public double scaleFactor = 1.;
	public String prjName;
	public Grid llGrid;
	public Grid grGrid;
	public ArrayList<MapPoint> calibrationPoints = new ArrayList<>();
	private transient LinearBinding binding;
	private transient OzfReader ozf;

	protected OzfMap()
	{
	}

	public OzfMap(String path)
	{
		super(path);
		binding = new LinearBinding();
	}

	@Override
	public void initialize()
	{
		// We do it twice because of deserialization
		binding = new LinearBinding();
		bind();
	}

	@Override
	public void destroy()
	{
	}

	@Override
	public synchronized void activate(OnMapTileStateChangeListener listener, double mpp, boolean current) throws Throwable
	{
		Log.d("OZI", "Image file specified: " + imagePath);
		File image = new File(imagePath);
		if (! image.exists())
		{
			imagePath = imagePath.replace("\\", "/");
			image = new File(imagePath);
			File map = new File(path);
			image = new File(map.getParentFile(), image.getName());
			if (! image.exists())
			{
				throw new FileNotFoundException("Image file not found: " + imagePath);
			}
		}
		Log.d("OZI", "Image file found: " + image.getCanonicalPath());
		ozf = new OzfReader(image);
		super.activate(listener, mpp, current);
	}

	@Override
	public synchronized void deactivate()
	{
		super.deactivate();
		//TODO This shouldn't happen but happens
		if (ozf != null)
			ozf.close();
		ozf = null;
	}
	
	public void addCalibrationPoint(MapPoint point)
	{
		calibrationPoints.add(point);
	}

	@Override
	public boolean getXYByLatLon(double lat, double lon, int[] xy)
	{
		double nn, ee;
		
        Point2D.Double src = new Point2D.Double(lon, lat);
        Point2D.Double dst = new Point2D.Double();
		projection.transform(src.x, src.y, dst);
		ee = dst.x;
		nn = dst.y;
		xy[0] = (int) Math.round(binding.Kx[0]*nn + binding.Kx[1]*ee + binding.Kx[2]);
		xy[1] = (int) Math.round(binding.Ky[0]*nn + binding.Ky[1]*ee + binding.Ky[2]);

		return (xy[0] >= 0 && xy[0] < width * zoom && xy[1] >= 0 && xy[1] < height * zoom);
	}

	@SuppressWarnings("UnusedDeclaration")
	public boolean getXYByEN(int e, int n, int[] xy)
	{
		xy[0] = (int) Math.round(binding.Kx[0]*n + binding.Kx[1]*e + binding.Kx[2]);
		xy[1] = (int) Math.round(binding.Ky[0]*n + binding.Ky[1]*e + binding.Ky[2]);

		return (xy[0] >= 0 && xy[0] < width * zoom && xy[1] >= 0 && xy[1] < height * zoom);
	}

	@SuppressWarnings("UnusedDeclaration")
	public boolean getENByXY(int x, int y, int[] en)
	{
		en[1] = (int) (binding.Klat[0]*x + binding.Klat[1]*y + binding.Klat[2]);
		en[0] = (int) (binding.Klon[0]*x + binding.Klon[1]*y + binding.Klon[2]);

		return (x >= 0 && x < width * zoom && y >= 0 || y < height * zoom);
	}

	@SuppressWarnings("UnusedDeclaration")
	public void getENByLatLon(double lat, double lon, int[] en)
	{
        Point2D.Double src = new Point2D.Double(lon, lat);
        Point2D.Double dst = new Point2D.Double();
		projection.transform(src.x, src.y, dst);
		en[0] = (int) dst.x;
		en[1] = (int) dst.y;
	}

	@Override
	public boolean getLatLonByXY(int x, int y, double[] ll)
	{
		double nn, ee;

		nn = binding.Klat[0]*x + binding.Klat[1]*y + binding.Klat[2];
		ee = binding.Klon[0]*x + binding.Klon[1]*y + binding.Klon[2];

        Point2D.Double src = new Point2D.Double(ee, nn);
        Point2D.Double dst = new Point2D.Double();
		projection.inverseTransform(src, dst);
		ll[0] = dst.y;
		ll[1] = dst.x;

		return (x >= 0 && x < width * zoom && y >= 0 || y < height * zoom);
	}

	public boolean coversScreen(int[] map_xy, int width, int height)
	{
		int w2 = width / 2;
		int h2 = height / 2;
		
		int l = (int) ((map_xy[0] - w2) / zoom);
		int t = (int) ((map_xy[1] - h2) / zoom);
		int r = (int) ((map_xy[0] + w2) / zoom);
		int b = (int) ((map_xy[1] + h2) / zoom);
		
		int j = cornerMarkers.length - 1;
		int oddTL = 0;
		int oddTR = 0;
		int oddBL = 0;
		int oddBR = 0;

		for (int i=0; i < cornerMarkers.length; i++)
		{
			if (cornerMarkers[i].y < t && cornerMarkers[j].y >= t || cornerMarkers[j].y < t && cornerMarkers[i].y >= t)
			{
				int tx = (int) (cornerMarkers[i].x + (t - cornerMarkers[i].y) * 1. / (cornerMarkers[j].y - cornerMarkers[i].y) * (cornerMarkers[j].x - cornerMarkers[i].x));
				if (tx < l)
				{
					oddTL++;
				}
				if (tx < r)
				{
					oddTR++;
				}
			}
			if (cornerMarkers[i].y < b && cornerMarkers[j].y >= b || cornerMarkers[j].y < b && cornerMarkers[i].y >= b)
			{
				int bx = (int) (cornerMarkers[i].x + (b - cornerMarkers[i].y) * 1. / (cornerMarkers[j].y - cornerMarkers[i].y) * (cornerMarkers[j].x - cornerMarkers[i].x));
				if (bx < l)
				{
					oddBL++;
				}
				if (bx < r)
				{
					oddBR++;
				}
			}
			j=i;
		}

		return (oddTL % 2 == 1) && (oddTR % 2 == 1) && (oddBL % 2 == 1) && (oddBR % 2 == 1);
	}

	@Override
	public int getScaledWidth()
	{
		return (int) (width * zoom);
	}

	@Override
	public int getScaledHeight()
	{
		return (int) (height * zoom);
	}

	@Override
	public double getMPP()
	{
		return mpp / getZoom();
	}

	@Override
	public double getCoveringRatio(double refMpp)
	{
		return refMpp / mpp;
	}

	@Override
	public synchronized void recalculateCache()
	{
		if (cache != null)
			cache.destroy();
		int nx = (int) Math.ceil(viewportWidth * 1. / ozf.tile_dx()) + 2;
		int ny = (int) Math.ceil(viewportHeight * 1. / ozf.tile_dy()) + 2;
		if (nx > ozf.tiles_per_x())
			nx = ozf.tiles_per_x();
		if (ny > ozf.tiles_per_y())
			ny = ozf.tiles_per_y();
		int cacheSize = nx * ny;
		Log.e("OZI", "Cache size: " + cacheSize);
		cache = new TileRAMCache(cacheSize);
		ozf.setCache(cache);
	}

	@Override
	public double getNextZoom()
	{
		double zoomCurrent = getZoom();
		double zoom = Double.NaN;
		for (int i = 0; i < zoomLevelsSupported.length; i++)
		{
			if (zoomLevelsSupported[i] > zoomCurrent)
			{
				zoom = zoomLevelsSupported[i];
				break;
			}
		}
		if (! Double.isNaN(zoom))
	    	return zoom;
		else
			return 0.0;
	}

	@Override
	public double getPrevZoom()
	{
		double zoomCurrent = getZoom();
		double zoom = Double.NaN;
		for (int i = zoomLevelsSupported.length - 1; i >= 0; i--)
		{
			if (zoomLevelsSupported[i] < zoomCurrent)
			{
				zoom = zoomLevelsSupported[i];
				break;
			}
		}
		if (! Double.isNaN(zoom))
	    	return zoom;
		else
			return 0.0;
	}

	@Override
	synchronized public double getZoom()
	{
		if (ozf != null)
			return ozf.getZoom();
		else
			return 1.;
	}

	@Override
	public synchronized void setZoom(double z)
	{
		Log.e("OZI", "[" + title + "] setZoom: " + z);
		zoom = ozf.setZoom(z);
		recalculateCache();
		bind();
		mapClipPath.rewind();
		mapClipPath.setLastPoint((float) (cornerMarkers[0].x * zoom), (float) (cornerMarkers[0].y * zoom));
		for (int i = 1; i < cornerMarkers.length; i++)
			mapClipPath.lineTo((float) (cornerMarkers[i].x * zoom), (float) (cornerMarkers[i].y * zoom));
		mapClipPath.close();		
	}

	@Override
	public void getMapCenter(double[] center)
	{
		int x = getScaledWidth() / 2;
		int y = getScaledHeight() / 2;
		getLatLonByXY(x, y, center);
	}

	synchronized public boolean drawMap(Viewport viewport, boolean cropBorder, boolean drawBorder, Canvas c) throws OutOfMemoryError
	{
		if (ozf == null)
			return false;
		int[] map_xy = new int[2];
		getXYByLatLon(viewport.mapCenter[0], viewport.mapCenter[1], map_xy);
		map_xy[0] -= viewport.lookAheadXY[0];
		map_xy[1] -= viewport.lookAheadXY[1];
		try
		{
			Path clipPath = new Path();
			if (cropBorder || drawBorder)
				mapClipPath.offset(-map_xy[0] + viewport.canvasWidth / 2, -map_xy[1] + viewport.canvasHeight / 2, clipPath);
            c.save();
			if (cropBorder)
				c.clipPath(clipPath);
			
			int[] cr = ozf.map_xy_to_cr(map_xy);
			int[] xy = ozf.map_xy_to_xy_on_tile(map_xy);
			
			int tile_w = ozf.tile_dx();
			int tile_h = ozf.tile_dy();
			
			if (tile_w == 0 || tile_h == 0)
			{
				c.restore();
				c.drawRGB(255, 0, 0);
				return false;
			}

			int c_min = (int) Math.floor(ozf.map_x_to_c(map_xy[0] - viewport.canvasWidth / 2));
			int c_max = (int) Math.ceil(ozf.map_x_to_c(map_xy[0] + viewport.canvasWidth / 2));
			
			int r_min = (int) Math.floor(ozf.map_y_to_r(map_xy[1] - viewport.canvasHeight / 2));
			int r_max = (int) Math.ceil(ozf.map_y_to_r(map_xy[1] + viewport.canvasHeight / 2));
			
			boolean result = true;
			
			if (c_min < 0)
			{
				c_min = 0;
				result = false;
			}
			if (r_min < 0)
			{
				r_min = 0;
				result = false;
			}
			if (c_max > ozf.tiles_per_x())
			{
				c_max = ozf.tiles_per_x();
				result = false;
			}
			if (r_max > ozf.tiles_per_y())
			{
				r_max = ozf.tiles_per_y();
				result = false;
			}
			
			int txb = viewport.canvasWidth / 2 - xy[0] - (cr[0] - c_min) * tile_w;
			int tyb = viewport.canvasHeight / 2 - xy[1] - (cr[1] - r_min) * tile_h;

			for (int i = r_min; i < r_max; i++)
			{
				for (int j = c_min; j < c_max; j++)
				{
					int tx = txb + (j - c_min) * tile_w;
					int ty = tyb + (i - r_min) * tile_h;
				
					Bitmap tile = ozf.tile_get(j, i);
					
					if (tile != null)
					{
						int tile_dx = ozf.tile_dx(j, i);
						int tile_dy = ozf.tile_dy(j, i);
						if (tile_dx < tile_w || tile_dy < tile_h)
						{
							Rect src = new Rect(0, 0, tile_dx, tile_dy);
							Rect dst = new Rect(tx, ty, tx + src.right, ty + src.bottom);
							c.drawBitmap(tile, src, dst, null);
						}
						else
						{
							c.drawBitmap(tile, tx, ty, null);
						}
					}
				}
			}
			c.restore();
			if (drawBorder)
				c.drawPath(clipPath, borderPaint);
			if (result)
				result = coversScreen(map_xy, viewport.canvasWidth, viewport.canvasHeight);
			return result;
		}
		catch (OutOfMemoryError err)
		{
			cache.clear();
			throw err;
		}
	}

	@Override
	public int getPriority()
	{
		return 1;
	}

	public void bind()
	{
		MapPoint[] points = new MapPoint[calibrationPoints.size()];

		int i = 0;
		for (MapPoint mp : calibrationPoints)
		{
			points[i] = new MapPoint();
			points[i].lat = mp.lat;
			points[i].lon = mp.lon;
			points[i].x = (int) (mp.x * zoom);
			points[i].y = (int) (mp.y * zoom);
	        Point2D.Double src = new Point2D.Double(points[i].lon, points[i].lat);
	        Point2D.Double dst = new Point2D.Double();
			projection.transform(src.x, src.y, dst);
			points[i].n = dst.y;
			points[i].e = dst.x;
//			Log.e("OZI","point transform: "+points[i].lat+" "+points[i].lon+" -> "+points[i].n+" "+points[i].e);
			src.x = dst.x;
			src.y = dst.y;
			projection.inverseTransform(src, dst);
//			Log.e("OZI","point reverse transform: "+src.y+" "+src.x+" -> "+dst.y+" "+dst.x);
			i++;
		}

		getKx(points);
		getKy(points);
		getKLat(points);
		getKLon(points);
	}

	private void getKx(MapPoint[] points)
	{
		double[][] a = new double[3][3];
		double[] b = new double[3];
		double[][] p = new double[3][points.length];

		int i = 0;
		for (MapPoint mp : points)
		{
			p[0][i] = mp.n;
			p[1][i] = mp.e;
			p[2][i] = mp.x;
			i++;
		}
		
		init_3x3(a, b, p, points.length);
		gauss(a, b, binding.Kx, 3);
		//Log.e("OZI", "Kx: "+binding.Kx[0]+","+binding.Kx[1]+","+binding.Kx[2]);
	}

	private void getKy(MapPoint[] points)
	{
		double[][] a = new double[3][3];
		double[] b = new double[3];
		double[][] p = new double[3][points.length];

		int i = 0;
		for (MapPoint mp : points)
		{
			p[0][i] = mp.n;
			p[1][i] = mp.e;
			p[2][i] = mp.y;
			i++;
		}

		init_3x3(a, b, p, points.length);
		gauss(a, b, binding.Ky, 3);
		//Log.e("OZI", "Ky: "+binding.Ky[0]+","+binding.Ky[1]+","+binding.Ky[2]);
	}

	private void getKLat(MapPoint[] points)
	{
		double[][] a = new double[3][3];
		double[] b = new double[3];
		double[][] p = new double[3][points.length];

		int i = 0;
		for (MapPoint mp : points)
		{
			p[0][i] = mp.x;
			p[1][i] = mp.y;
			p[2][i] = mp.n;
			i++;
		}
		
		init_3x3(a, b, p, points.length);
		gauss(a, b, binding.Klat, 3);
		//Log.e("OZI", "Klat: "+binding.Klat[0]+","+binding.Klat[1]+","+binding.Klat[2]);
	}

	private void getKLon(MapPoint[] points)
	{
		double[][] a = new double[3][3];
		double[] b = new double[3];
		double[][] p = new double[3][points.length];

		int i = 0;
		for (MapPoint mp : points)
		{
			p[0][i] = mp.x;
			p[1][i] = mp.y;
			p[2][i] = mp.e;
			i++;
		}

		init_3x3(a, b, p, points.length);
		gauss(a, b, binding.Klon, 3);
		//Log.e("OZI", "Klon: "+binding.Klon[0]+","+binding.Klon[1]+","+binding.Klon[2]);
	}

	/**
	 *  Solves linear equation.  Finds vector x such that ax = b.
	 *
	 *	@param a nXn matrix
	 *	@param b vector size n
	 *	@param x vector size n
	 *	@param n number of variables (size of vectors) (must be > 1)
	 *
	 *	This function will alter a and b, and put the solution in x.
	 *	@return true if the solution was found, false otherwise.
	 */
	private boolean gauss(double[][] a, double[] b, double[] x, int n)
	{
		int i,j,k;
		int ip = 0, kk, jj;
		double temp;
		double pivot;
		double q;

		/*
		 *	transform matrix to echelon form.
		 */
		for (i = 0; i < n-1; i++)
		{
			/*
			 *	Find the pivot.
			 */
			pivot = 0.0;
		    for (j = i; j < n; j++)
		    {
		    	temp = Math.abs(a[j][i]);
		    	if (temp > pivot)
		    	{
		    		pivot = temp;
		    		ip = j;
		    	}
		    }

		    if (pivot < 1.E-14)
		    {
		    	/*
		    	 *   Error - singular matrix.
		    	 */
		    	return false;
		    }

		    /*
		     *	Move the pivot row to the ith position
		     */
		    if (ip != i)
		    {
		    	double[] temp_p = a[i];
		    	a[i] = a[ip];
		    	a[ip] = temp_p;
		    	temp = b[i];
		    	b[i] = b[ip];
		    	b[ip] = temp;
		    }

		    /*
		     *	Zero entries below the diagonal.
		     */
		    for (k = i + 1; k < n; k++)
		    {
		    	q = -a[k][i] / a[i][i];

		    	a[k][i] = 0.0;

		    	for (j = i + 1; j < n; j++)
		    		a[k][j] = q * a[i][j] + a[k][j];
		    	b[k] = q * b[i] + b[k];
		    }

		}

		if (Math.abs(a[n-1][n-1]) < 1.E-14)
		{
			return false;
		}

		/*
		 *	Backsolve to obtain solution vector x.
		 */
		kk = n - 1;
		x[kk] = b[kk] / a[kk][kk];
		for (k = 0; k < n - 1; k++)
		{
			kk = n - k - 2;
			q = 0.0;

			for (j = 0; j <= k; j++)
			{
				jj = n - j - 1;
				q = q + a[kk][jj] * x[jj];
			}
			x[kk] = (b[kk] - q) / a[kk][kk];
		}

		return true;
	}

	private void init_3x3(double[][] a, double[] b, double[][] p, int size)
	{
		for (int i = 0; i < 3; i++)
		{
			b[i] = 0;
			
			for (int j = 0; j < 3; j++)
				a[i][j] = 0;
		}

		for(int i = 0; i < size; i++)
		{
			a[0][0] += p[0][i] * p[0][i];
			a[0][1] += p[0][i] * p[1][i];
			a[0][2] += p[0][i];
			a[1][1] += p[1][i] * p[1][i];
			a[1][2] += p[1][i];
			b[0] += p[2][i] * p[0][i];
			b[1] += p[2][i] * p[1][i];
			b[2] += p[2][i];
		}

		a[1][0] = a[0][1];
		a[2][0] = a[0][2];
		a[2][1] = a[1][2];
		a[2][2] = size;
	}
	
	private static class LinearBinding
	{
		double[] Kx = new double[3];
		double[] Ky = new double[3];
		double[] Klat = new double[3];
		double[] Klon = new double[3];
	}

	
	public void debug()
	{
		List<String> info = info();
		for (String line : info)
		{
			Log.d("OZI", line);
		}
	}

	@Override
	public List<String> info()
	{
		ArrayList<String> info = new ArrayList<>();
		
		info.add("title: " + title);
		if (projection != null)
		{
			info.add("projection: " + prjName + " (" + projection.getEPSGCode() + ")");
			info.add("\t" + projection.getPROJ4Description());
			info.add("ellipsoid: " + projection.getEllipsoid().toString());
		}
		if (origDatum != null)
		{
			info.add("datum: " + origDatum + " -> " + datum);
			info.add("  (coordinates shown in " + datum + ")");
		}
		else
		{
			info.add("datum: " + datum);
		}
		info.add("mpp: " + mpp);
		info.add("image width: " + width);
		info.add("image height: " + height);
		info.add("image file: " + imagePath);
		info.add("scale factor: " + 1 / scaleFactor);
		info.add("calibration points:");
		
		int i = 1;
		for (MapPoint mp : calibrationPoints)
		{
			info.add(String.format("\t%02d: x: %d y: %d lat: %f lon: %f", i, mp.x, mp.y, mp.lat, mp.lon));
			i++;
		}
		double[] ll = new double[2];
		getLatLonByXY(width/2, height/2, ll);
		info.add("map center (calibration) test: "+ll[0] + " " + ll[1]);
		
		info.add("corners:");

		if (cornerMarkers != null)
		{
			i = 1;
			for (MapPoint mp : cornerMarkers)
			{
				info.add(String.format("\t%02d: x: %d y: %d lat: %f lon: %f", i, mp.x, mp.y, mp.lat, mp.lon));
				i++;
			}
		}
		
		if (llGrid != null)
		{
			info.add("lat/lon grid:");
			info.add("  enabled: " + llGrid.enabled);
			info.add("  spacing: " + llGrid.spacing);
			info.add("  autoscale: " + llGrid.autoscale);
			info.add("  deg. color: " + llGrid.color1);
			info.add("  min. color: " + llGrid.color2);
			info.add("  sec. color: " + llGrid.color3);
			info.add("  label spacing: " + llGrid.labelSpacing);
			info.add("  label foreground: " + llGrid.labelForeground);
			info.add("  label background: " + llGrid.labelBackground);
			info.add("  label size: " + llGrid.labelSize);
			info.add("  label everywhere: " + llGrid.labelShowEverywhere);
		}

		if (grGrid != null)
		{
			info.add("other grid:");
			info.add("  enabled: " + grGrid.enabled);
			info.add("  spacing: " + grGrid.spacing);
			info.add("  autoscale: " + grGrid.autoscale);
			info.add("  km color: " + grGrid.color1);
			info.add("  meter color: " + grGrid.color2);
			info.add("  label spacing: " + grGrid.labelSpacing);
			info.add("  label foreground: " + grGrid.labelForeground);
			info.add("  label background: " + grGrid.labelBackground);
			info.add("  label size: " + grGrid.labelSize);
			info.add("  label everywhere: " + grGrid.labelShowEverywhere);
		}
		
		return info;
	}
}
