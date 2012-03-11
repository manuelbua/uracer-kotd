package com.bitfire.uracer.effects.postprocessing;

import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;

public class PingPongBuffer
{
	public final FrameBuffer buffer1, buffer2;
	public Texture texture1, texture2;

	public final int width, height;
	public PingPongBuffer( int width, int height, Format frameBufferFormat, boolean hasDepth )
	{
		this.width = width;
		this.height = height;

		buffer1 = new FrameBuffer( frameBufferFormat, width, height, hasDepth );
		buffer2 = new FrameBuffer( frameBufferFormat, width, height, hasDepth );

//		buffer1.setFilter( TextureFilter.Nearest, TextureFilter.Nearest );
//		buffer2.setFilter( TextureFilter.Nearest, TextureFilter.Nearest );

		rebind();
	}

	public PingPongBuffer( FrameBuffer buffer1, FrameBuffer buffer2 )
	{
		this.width = this.buffer1.getWidth();
		this.height = this.buffer1.getHeight();

		this.buffer1 = buffer1;
		this.buffer2 = buffer2;

		rebind();
	}

	public void dispose()
	{
		buffer1.dispose();
		buffer2.dispose();
	}

	public void rebind()
	{
		texture1 = buffer1.getColorBufferTexture();
		texture2 = buffer2.getColorBufferTexture();
	}

	/**
	 * begin ping-ponging, tex1#0, begin write to buf2
	 */
	public void begin()
	{
		texture1.bind(0);
		buffer2.begin();
	}

	/**
	 * swap, end write buf2, tex2#0, begin write to buf1
	 */
	public void swap()
	{
		buffer2.end();
		texture2.bind(0);
		buffer1.begin();
	}

	/**
	 * end, end write to buf1
	 */
	public void end()
	{
		buffer1.end();
	}
}
