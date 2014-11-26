package com.androzic.util;

public class MeanValue
{
	private double K = 0;
	private double n = 0;
	private double Ex = 0;
	private double Ex2 = 0;

	public void addValue(double x)
	{
		if (n == 0)
		{
			K = x;
		}
		n = n + 1;
		Ex = Ex + (x - K);
		Ex2 = Ex2 + (x - K) * (x - K);
	}
			 
	public void removeValue(double x)
	{
		n = n - 1;
		Ex = Ex - (x - K);
		Ex2 = Ex2 - (x - K) * (x - K);
	}
			 
	public double getMeanValue()
	{
		return K + Ex / n;
	}
			 
	public double getVariance()
	{
		return (Ex2 - (Ex*Ex)/n) / (n-1);
	}
}
