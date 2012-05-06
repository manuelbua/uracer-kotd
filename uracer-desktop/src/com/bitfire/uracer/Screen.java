package com.bitfire.uracer;

import java.util.Random;

public abstract class Screen {
	protected static Random random = new Random();

	public abstract void init( ScalingStrategy scalingStrategy );

	public abstract void removed();

	public abstract void pause();

	public abstract void resume();

	public abstract void tick();

	public abstract void update();

	public abstract void render();

	public abstract boolean quit();

	/** This debug call will gets called *after* tick and render are raised for all the entities, but
	 * the computational time will not be part of the cumulative time statistics */
	public abstract void debugUpdate();
}
