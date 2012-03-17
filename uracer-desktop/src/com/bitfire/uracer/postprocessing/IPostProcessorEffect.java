package com.bitfire.uracer.postprocessing;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;


public interface IPostProcessorEffect
{
	public void dispose();
	public void resume();
	public void render(final FrameBuffer scene);
}
