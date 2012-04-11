package com.bitfire.uracer;

import java.util.Random;

public abstract class Screen {
	protected static Random random = new Random();

	public abstract void init();

	public abstract void removed();

	public abstract void pause();

	public abstract void resume();

	public abstract void render();

	public abstract void tick();

	public abstract boolean quit();
}
