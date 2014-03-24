
package com.bitfire.uracer.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Disposable;

public abstract class Screen implements Disposable {

	public abstract boolean init ();

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
}
