package com.bitfire.uracer.postprocessing.filters;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.bitfire.uracer.postprocessing.IFilter;
import com.bitfire.uracer.postprocessing.PingPongBuffer;

public abstract class Filter<T> extends IFilter
{
	protected static final int u_texture_1 = 0;
	protected static final int u_texture_2 = 1;

	protected Texture inputTexture = null;
	protected FrameBuffer outputBuffer = null;

	public T setInput(Texture input)
	{
		this.inputTexture = input;
		return (T)this;
	}

	public T setInput(FrameBuffer input)
	{
		this.inputTexture = input.getColorBufferTexture();
		return (T)this;
	}

	public T setInput(PingPongBuffer input)
	{
		this.inputTexture = input.capture();
		return (T)this;
	}

	public T setOutput(FrameBuffer output)
	{
		this.outputBuffer = output;
		return (T)this;
	}

//	public T setOutput(PingPongBuffer output)
//	{
//		this.outputBuffer = output.getSourceBuffer();
//		return (T)this;
//	}

	public abstract void upload();
	protected abstract void compute();

	public void render()
	{
		if(outputBuffer!=null)
		{
			outputBuffer.begin();
			compute();
			outputBuffer.end();
		}
		else
			compute();
	}
}
