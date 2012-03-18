package com.bitfire.uracer.postprocessing;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.utils.Array;

public final class PostProcessor
{
	private final PingPongBuffer processorBuffer;
	private final Format fbFormat;
	private boolean capturing = false;
	public Array<PostProcessorEffect> effects = new Array<PostProcessorEffect>();
	private Color clearColor = Color.CLEAR;

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

		processorBuffer = new PingPongBuffer( fboWidth, fboHeight, fbFormat, useDepth );

		capturing = false;
	}

	public void dispose()
	{
		for(int i = 0; i < effects.size; i++)
			effects.get(i).dispose();

		effects.clear();

		processorBuffer.dispose();
	}

	public void addEffect(PostProcessorEffect effect)
	{
		effects.add( effect );
	}

	public void removeEffect(PostProcessorEffect effect)
	{
		effects.removeValue( effect, false );
	}

	public Format getFramebufferFormat()
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
			processorBuffer.begin();
			processorBuffer.capture();

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
			processorBuffer.end();
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
			processorBuffer.begin();
			processorBuffer.capture();
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
			processorBuffer.end();
		}
	}

	/**
	 * call this when resuming
	 */
	public void resume()
	{
		for(int i = 0; i < effects.size; i++)
			effects.get(i).resume();
	}

	/**
	 * Finish capturing the scene, post-process and render the effects, if any
	 */
	public void render()
	{
		captureEnd();

		if(effects.size>0)
		{
			for(int i = 0, last = effects.size-2; i < effects.size-1 && last >= 0; i++)
			{
				PostProcessorEffect e = effects.get( i );

				processorBuffer.capture();
				{
					e.render( processorBuffer.getSourceBuffer(), processorBuffer.getResultBuffer() );
				}
			}

			processorBuffer.end();

			PostProcessorEffect last = effects.get( effects.size-1 );
			last.render( processorBuffer.getResultBuffer(), null );
		}
	}
}
