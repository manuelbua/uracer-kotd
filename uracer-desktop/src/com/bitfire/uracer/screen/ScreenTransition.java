
package com.bitfire.uracer.screen;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.screen.ScreenFactory.ScreenId;

public abstract class ScreenTransition implements Disposable {
	private ScreenFactory screenFactory;

	public ScreenTransition (ScreenFactory factory) {
		screenFactory = factory;
	}

	protected Screen createScreen (ScreenId screenId) {
		return screenFactory.createScreen(screenId);
	}

	public void pause () {
	}

	public void resume () {
	}

	/** Called before the transition is started. */
	public abstract void frameBuffersReady (Screen current, FrameBuffer from, ScreenId next, FrameBuffer to);

	/** Called when the transition is finished. */
	public abstract Screen nextScreen ();

	public abstract void setDuration (long durationMs);

	public abstract boolean isComplete ();

	public abstract void reset ();

	@Override
	public abstract void dispose ();

	public abstract void update ();

	public abstract void render ();
}
