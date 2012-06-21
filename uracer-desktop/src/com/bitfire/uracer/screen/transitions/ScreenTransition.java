package com.bitfire.uracer.screen.transitions;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Disposable;

public abstract class ScreenTransition implements Disposable {
	public abstract void init( FrameBuffer curr, FrameBuffer next );

	public abstract boolean hasFinished();

	@Override
	public abstract void dispose();

	public abstract void pause();

	public abstract void resume();

	public abstract void update();

	public abstract void render();
}
