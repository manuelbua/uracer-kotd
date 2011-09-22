package com.bitfire.uracer;

public class Lap
{
	private long startNs;

	public Lap()
	{
		startNs = 0;
	}

	public Lap set(Lap lap)
	{
		this.startNs = lap.startNs;
		return this;
	}

	public Lap set( long nanoTime )
	{
		startNs = nanoTime;
		return this;
	}

	public void start()
	{
		startNs = System.nanoTime();
	}

	public float getElapsedSeconds()
	{
		return (float)(System.nanoTime() - startNs) / 1000000000f;
	}

	public long getStartNanotime()
	{
		return startNs;
	}
}
