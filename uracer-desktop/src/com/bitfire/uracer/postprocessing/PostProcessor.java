package com.bitfire.uracer.postprocessing;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.utils.Array;

public final class PostProcessor
{
	private final PingPongBuffer composite;
	private final Format fbFormat;
	private boolean capturing = false;
	public Array<PostProcessorEffect> effects = new Array<PostProcessorEffect>();
	private Color clearColor = Color.CLEAR;
	private static Array<PingPongBuffer> buffers = new Array<PingPongBuffer>(5);

	public PostProcessor( int fboWidth, int fboHeight, boolean useDepth, boolean useAlphaChannel, boolean use32Bits)
	{
		if( use32Bits )
		{
			if( useAlphaChannel )
			{
				fbFormat = Format.RGBA8888;
			} else
			{
				fbFormat = Format.RGB888;
			}
		} else
		{
			if( useAlphaChannel )
			{
				fbFormat = Format.RGBA4444;
			} else
			{
				fbFormat = Format.RGB565;
			}
		}

		composite = newPingPongBuffer( fboWidth, fboHeight, fbFormat, useDepth );

		capturing = false;
	}

	/**
	 * Create and returns a managed PingPongBuffer buffer, just create and forget.
	 * This is a drop-in replacement for the same-signature constructor.
	 */
	public static final PingPongBuffer newPingPongBuffer( int width, int height, Format frameBufferFormat, boolean hasDepth )
	{
		PingPongBuffer buffer = new PingPongBuffer( width, height, frameBufferFormat, hasDepth );
		buffers.add( buffer );
		return buffer;
	}

	public void dispose()
	{
		for(int i = 0; i < effects.size; i++)
			effects.get(i).dispose();

		effects.clear();

		// cleanup managed buffers, if any
		for(int i = 0; i < buffers.size; i++)
			buffers.get(i).dispose();

		buffers.clear();
	}

	public void addEffect(PostProcessorEffect effect)
	{
		effects.add( effect );
	}

	public void removeEffect(PostProcessorEffect effect)
	{
		effects.removeValue( effect, false );
	}

	public final Format getFramebufferFormat()
	{
		return fbFormat;
	}

	public void setClearColor(Color color)
	{
		clearColor.set( color );
	}

	public void setClearColor( float r, float g, float b, float a )
	{
		clearColor.set( r, g, b, a );
	}

	/**
	 * Start capturing the scene
	 */
	public void capture()
	{
		if(!capturing)
		{
			capturing = true;
			composite.begin();
			composite.capture();

			Gdx.gl.glClearColor( clearColor.r, clearColor.g, clearColor.b, clearColor.a );
			Gdx.gl.glClear( GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT );
		}
	}

	/**
	 * Pause capturing
	 */
	public void capturePause()
	{
		if(capturing)
		{
			capturing = false;
			composite.end();
		}
	}

	/**
	 * Start capturing again, after pause
	 */
	public void captureContinue()
	{
		if(!capturing)
		{
			capturing = true;
			composite.begin();
			composite.capture();
		}
	}

	/**
	 * Stops capturing the scene
	 */
	public void captureEnd()
	{
		if(capturing)
		{
			capturing = false;
			composite.end();
		}
	}

	/**
	 * call this when resuming
	 */
	public void resume()
	{
		for(int i = 0; i < effects.size; i++)
			effects.get(i).resume();

		for(int i = 0; i < buffers.size; i++)
			buffers.get(i).rebind();
	}

	/**
	 * Finish capturing the scene, post-process w/ the effect chain, if any
	 */
	public void render()
	{
		captureEnd();

		if(effects.size>0)
		{
			// render effects chain, [0,n-1]
			for(int i = 0; i < effects.size-1; i++)
			{
				PostProcessorEffect e = effects.get( i );

				composite.capture();
				{
					e.render( composite.getSourceBuffer(), composite.getResultBuffer() );
				}
			}

			// complete
			composite.end();

			// render with null dest, to the screen!
			effects.get( effects.size-1 ).render( composite.getResultBuffer(), null );
		}
	}
}
