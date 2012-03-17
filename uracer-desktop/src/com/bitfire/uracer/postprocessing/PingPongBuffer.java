package com.bitfire.uracer.postprocessing;

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

	private Texture nextPingpongTexSrc, lastPingpongTexDst, currTexSrc;
	private FrameBuffer nextPingpongBufSrc, lastPingpongBufDst, currBufSrc;

	public int width, height;

	public PingPongBuffer( int width, int height, Format frameBufferFormat, boolean hasDepth )
	{
		set( new FrameBuffer( frameBufferFormat, width, height, hasDepth ), new FrameBuffer( frameBufferFormat, width, height, hasDepth ) );
	}

	public PingPongBuffer( FrameBuffer buffer1, FrameBuffer buffer2 )
	{
		set( buffer1, buffer2 );
	}

	// TODO +unset, check for "ownage" of the buffers for correct impl of unset/set/multiple set/etc..
	public void set( FrameBuffer buffer1, FrameBuffer buffer2 )
	{
		this.buffer1 = buffer1;
		this.buffer2 = buffer2;
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

		nextPingpongTexSrc = currTexSrc = texture1; nextPingpongBufSrc = currBufSrc = buffer1;
		lastPingpongTexDst = texture2; lastPingpongBufDst = buffer2;
	}

	private boolean writeState, pending1, pending2;

	/**
	 * Both starts and continue ping-ponging between two buffers, returning the previous
	 * buffer containing the last result, initiating recording on the next buffer.
	 */
	public Texture capture()
	{
		endPending();

		if( writeState )
		{
			// the caller is performing a pingPong step, this is the current source texture
			currTexSrc = texture1;
			currBufSrc = buffer1;

			// this will be the next pingPong step's source texture
			nextPingpongTexSrc = lastPingpongTexDst = texture2;
			nextPingpongBufSrc = lastPingpongBufDst = buffer2;

			// write to buf2
			pending2 = true;
			buffer2.begin();
		} else
		{
			currTexSrc = texture2;
			currBufSrc = buffer2;

			nextPingpongTexSrc = lastPingpongTexDst = texture1;
			nextPingpongBufSrc = lastPingpongBufDst = buffer1;

			// write to buf1
			pending1 = true;
			buffer1.begin();
		}

		writeState = !writeState;
		return currTexSrc;
	}

	public Texture getCurrentSouceTexture()
	{
		return currTexSrc;
	}

	public FrameBuffer getCurrentSourceBuffer()
	{
		return currBufSrc;
	}

	/**
	 * @return Returns the next buffer's texture that will be used as a source when
	 * the next "next()" step will be performed.
	 */
	public Texture getNextSourceTexture()
	{
		return nextPingpongTexSrc;
	}

	/**
	 * @return Returns the next buffer that will be used as a source when
	 * the next "next()" step will be performed.
	 */
	public FrameBuffer getNextSourceBuffer()
	{
		return nextPingpongBufSrc;
	}

	/**
	 * @return Returns the result of the latest {@link #capture()}. Texture version.
	 */
	public Texture getLastDestinationTexture()
	{
		return lastPingpongTexDst;
	}

	/**
	 * @return Returns the result of the latest {@link #capture()}. Buffer version.
	 */
	public FrameBuffer getLastDestinationBuffer()
	{
		return lastPingpongBufDst;
	}

	/**
	 * Ensures the initial buffer state is always the same before starting ping-ponging.
	 */
	public void begin()
	{
		rebind();
	}

	/**
	 * Finishes ping-ponging, must always be called after a call to {@link #capture()}
	 */
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
