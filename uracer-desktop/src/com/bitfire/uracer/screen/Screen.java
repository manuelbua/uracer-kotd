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

	public void beforeRender( float timeAliasingFactor )
	{
	}

	public abstract void render( float timeAliasingFactor );

	public void tick()
	{
	}
}
