package com.bitfire.uracer.screen.transitions;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Disposable;

public abstract class ScreenTransition implements Disposable {
	public abstract void setupFrameBuffers( FrameBuffer curr, FrameBuffer next );

	public abstract void setDuration( long durationMs );

	public abstract boolean isComplete();

	public abstract void reset();

	@Override
	public abstract void dispose();

	public void pause() {
	}

	public void resume() {
	}

	public abstract void update();

	public abstract void render();
}
