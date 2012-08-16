
package com.bitfire.uracer.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.ScalingStrategy;

public abstract class Screen implements Disposable {

	public abstract void init (ScalingStrategy scalingStrategy);

	public void enable () {
	}

	public void disable () {
		Gdx.input.setInputProcessor(null);
	}

	public void resize (int width, int height) {
	}

	public abstract void pause ();

	public abstract void resume ();

	public abstract void tick ();

	public abstract void tickCompleted ();

	public abstract void render (FrameBuffer dest);

	/** This debug call will gets called *after* tick and render are raised for all the entities, but the computational time will
	 * not be part of the cumulative time statistics */
	public void debugRender () {
	};
}
