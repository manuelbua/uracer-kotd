package com.bitfire.uracer.effects.postprocessing.filters;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;

public abstract class Filter
{
	public abstract void render(Texture source);

	public void render(Texture source, FrameBuffer dest)
	{
		dest.begin();
		render(source);
		dest.end();
	}
}
