package com.bitfire.uracer;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;

public abstract class Screen {
	public abstract void init( ScalingStrategy scalingStrategy );

	public abstract void removed();

	public abstract void pause();

	public abstract void resume();

	public abstract void tick();

	public abstract void tickCompleted();

	public abstract void render( FrameBuffer dest );

	public abstract boolean quit();

	/** This debug call will gets called *after* tick and render are raised for all the entities, but
	 * the computational time will not be part of the cumulative time statistics */
	public abstract void debugUpdate();
}
