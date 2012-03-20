package com.bitfire.uracer.postprocessing;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

/**
 * Provides a way to capture the rendered scene to an off-screen buffer
 * and to apply a chain of effects on it before rendering to screen.
 *
 * Effects can be added or removed via {@link #addEffect(PostProcessorEffect)}
 * and {@link #removeEffect(PostProcessorEffect)}.
 */
public final class PostProcessor implements Disposable
{
	private final PingPongBuffer composite;
	private final Format fbFormat;
	private boolean capturing = false;
	private Array<PostProcessorEffect> effects = new Array<PostProcessorEffect>();
	private Color clearColor = Color.CLEAR;
	private static Array<PingPongBuffer> buffers = new Array<PingPongBuffer>(5);

	/**
	 * Construct a new PostProcessor object with the given parameters.
	 */
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
	 * Creates and returns a managed PingPongBuffer buffer, just create and forget.
	 * This is a drop-in replacement for the same-signature constructor.
	 */
	public static final PingPongBuffer newPingPongBuffer( int width, int height, Format frameBufferFormat, boolean hasDepth )
	{
		PingPongBuffer buffer = new PingPongBuffer( width, height, frameBufferFormat, hasDepth );
		buffers.add( buffer );
		return buffer;
	}

	/**
	 * Frees owned resources.
	 */
	@Override
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

	/**
	 * Adds the specified effect to the effect chain: the order IS important
	 * since effects will be applied in a FIFO fashion, the first added
	 * is the first being applied.
	 */
	public void addEffect(PostProcessorEffect effect)
	{
		effects.add( effect );
	}

	/**
	 * Removes the specified effect from the effect chain.
	 */
	public void removeEffect(PostProcessorEffect effect)
	{
		effects.removeValue( effect, false );
	}

	/**
	 * Returns the internal framebuffer format, computed from the
	 * parameters specified during construction.
	 */
	public final Format getFramebufferFormat()
	{
		return fbFormat;
	}

	/**
	 * Sets the color that will be used to clear the buffer.
	 */
	public void setClearColor(Color color)
	{
		clearColor.set( color );
	}

	/**
	 * Sets the color that will be used to clear the buffer.
	 */
	public void setClearColor( float r, float g, float b, float a )
	{
		clearColor.set( r, g, b, a );
	}

	/**
	 * Starts capturing the scene, clears the buffer with the clear
	 * color specified by {@link #setClearColor(Color)} or
	 * {@link #setClearColor(float r, float g, float b, float a)}.
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
	 * Pauses capturing
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
	 * Starts capturing again, after a pause, without clearing the screen.
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
	 * Stops capturing the scene.
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
	 * Regenerates and/or rebinds owned resources when is needed, eg. when
	 * the OpenGL context is lost.
	 */
	public void rebind()
	{
		for(int i = 0; i < effects.size; i++)
			effects.get(i).rebind();

		for(int i = 0; i < buffers.size; i++)
			buffers.get(i).rebind();
	}

	/**
	 * Stops capturing the scene and apply the effect chain, if there is one.
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