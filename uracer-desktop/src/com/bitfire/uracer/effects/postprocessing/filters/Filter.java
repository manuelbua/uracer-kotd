package com.bitfire.uracer.effects.postprocessing.filters;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.bitfire.uracer.effects.postprocessing.FullscreenQuad;

public abstract class Filter
{
	public abstract void render(FullscreenQuad quad, Texture source);

	public void render(FullscreenQuad quad, Texture source, FrameBuffer dest)
	{
		dest.begin();
		render(quad, source);
		dest.end();
	}
}
