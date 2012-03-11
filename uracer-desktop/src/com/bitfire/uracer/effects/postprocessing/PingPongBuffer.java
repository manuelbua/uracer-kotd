package com.bitfire.uracer.effects.postprocessing;

import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;

/**
 * Encapsulates a ping-pong buffer.
 *
 * @author manuel
 *
 */
public class PingPongBuffer
{
	public FrameBuffer buffer1, buffer2;
	public Texture texture1, texture2;

	private Texture nextPingpongTexSrc, lastPingpongTexDst;
	private FrameBuffer nextPingpongBufSrc, lastPingpongBufDst;

	public int width, height;

	public PingPongBuffer( int width, int height, Format frameBufferFormat, boolean hasDepth )
	{
		set( new FrameBuffer( frameBufferFormat, width, height, hasDepth ), new FrameBuffer( frameBufferFormat, width, height, hasDepth ) );
	}

	public PingPongBuffer( FrameBuffer buffer1, FrameBuffer buffer2 )
	{
		set( buffer1, buffer2 );
	}

	public void set( FrameBuffer buffer1, FrameBuffer buffer2 )
	{
		this.buffer1 = buffer1;
		this.buffer2 = buffer2;

		// buffer1.setFilter( TextureFilter.Nearest, TextureFilter.Nearest );
		// buffer2.setFilter( TextureFilter.Nearest, TextureFilter.Nearest );

		this.width = this.buffer1.getWidth();
		this.height = this.buffer1.getHeight();

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
		restore();
	}

	private void restore()
	{
		pending1 = pending2 = false;
		writeState = true;

		nextPingpongTexSrc = texture1; nextPingpongBufSrc = buffer1;
		lastPingpongTexDst = texture2; lastPingpongBufDst = buffer2;
	}

	private boolean writeState, pending1, pending2;

	/**
	 * Start/continue ping-ponging between two buffers.
	 *
	 * Returns the result of the previous pass, or the source texture for the next pass if the ping-ponging was ended.
	 */
	public Texture pingPong()
	{
		endPending();

		Texture currSource = null;
		if( writeState )
		{
			// the caller is performing a pingPong step, this is the current source texture
			currSource = texture1;

			// this will be the next pingPong step's source texture
			nextPingpongTexSrc = lastPingpongTexDst = texture2;
			nextPingpongBufSrc = lastPingpongBufDst = buffer2;

			// write to buf2
			pending2 = true;
			buffer2.begin();
		} else
		{
			currSource = texture2;

			nextPingpongTexSrc = lastPingpongTexDst = texture1;
			nextPingpongBufSrc = lastPingpongBufDst = buffer1;

			// write to buf1
			pending1 = true;
			buffer1.begin();
		}

		writeState = !writeState;
		return currSource;
	}

	public Texture getNextSourceTexture()
	{
		return nextPingpongTexSrc;
	}

	public FrameBuffer getNextSourceBuffer()
	{
		return nextPingpongBufSrc;
	}

	public Texture getLastDestinationTexture()
	{
		return lastPingpongTexDst;
	}

	public FrameBuffer getLastDestinationBuffer()
	{
		return lastPingpongBufDst;
	}

	public void end()
	{
		endPending();
	}

	private void endPending()
	{
		if( pending1 )
		{
			buffer1.end();
			pending1 = false;
		}

		if( pending2 )
		{
			buffer2.end();
			pending2 = false;
		}
	}
}
