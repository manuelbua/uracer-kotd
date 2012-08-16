
package com.bitfire.uracer.screen.transitions;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.screen.Screen;
import com.bitfire.uracer.screen.ScreenFactory.ScreenType;

public abstract class ScreenTransition implements Disposable {
	/** Called before the transition is started. */
	public abstract void frameBuffersReady (Screen current, FrameBuffer from, ScreenType next, FrameBuffer to);

	/** Called when the transition is finished. */
	public abstract Screen nextScreen ();

	public abstract void setDuration (long durationMs);

	public abstract boolean isComplete ();

	public abstract void reset ();

	@Override
	public abstract void dispose ();

	public void pause () {
	}

	public void resume () {
	}

	public abstract void update ();

	public abstract void render ();
}
