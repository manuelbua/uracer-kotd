package com.bitfire.uracer.postprocessing;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.bitfire.uracer.utils.Hash;


public abstract class PostProcessorEffect
{
	public final long id;
	public final String name;

	public PostProcessorEffect()
	{
		this.name = this.getClass().getSimpleName();
		this.id = Hash.RSHash( this.name );
	}

	@Override
	public boolean equals(Object obj)
	{
		if( !(obj instanceof PostProcessorEffect))
		{
			return false;
		}

		PostProcessorEffect e = (PostProcessorEffect)obj;
		return e.id == this.id;
	};

	public abstract void dispose();
	public abstract void resume();
	public abstract void render(final FrameBuffer src, final FrameBuffer dest);
}
