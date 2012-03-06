package com.bitfire.uracer.screen;

import java.util.Random;

import com.bitfire.uracer.URacer;

public abstract class Screen
{
	protected static Random random = new Random();
	private URacer uracer;

	public final void init( URacer uracer )
	{
		this.uracer = uracer;
	}

	public void removed()
	{
	}

	protected void setScreen( Screen screen )
	{
		uracer.setScreen( screen );
	}

	public abstract void render();
	public abstract void tick();
	public abstract boolean quit();
}
