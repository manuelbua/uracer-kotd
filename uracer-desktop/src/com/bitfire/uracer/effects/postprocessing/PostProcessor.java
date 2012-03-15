package com.bitfire.uracer.effects.postprocessing;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;

public final class PostProcessor
{
	private final FrameBuffer bufferScene;
	private final Format fbFormat;
	private final FullscreenQuad fullScreenQuad;
	private boolean capturing = false;
	private IPostProcessorEffect effect = null;

	private Texture textureScene;

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

		bufferScene = new FrameBuffer( fbFormat, fboWidth, fboHeight, useDepth );
		textureScene = bufferScene.getColorBufferTexture();

		fullScreenQuad = new FullscreenQuad();
		capturing = false;
	}

	public void dispose()
	{
		if(effect != null)
			effect.dispose();

		bufferScene.dispose();
		fullScreenQuad.dispose();
	}

	public void setEffect(IPostProcessorEffect effect)
	{
		this.effect = effect;
	}

	public Format getFramebufferFormat()
	{
		return fbFormat;
	}

	/**
	 * Start capturing the scene
	 */
	public void capture()
	{
		if(!capturing && ( effect != null ))
		{
			capturing = true;
			bufferScene.begin();

			Color c = Color.CLEAR;
			if(effect != null)
				c = effect.getClearColor();

			Gdx.gl.glClearColor( c.r, c.g, c.b, c.a );
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
			bufferScene.end();
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
			bufferScene.begin();
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
			bufferScene.end();
		}
	}

	/**
	 * call this when resuming
	 */
	public void resume()
	{
		textureScene = bufferScene.getColorBufferTexture();
		if( effect != null )
			effect.resume();
	}

	/**
	 * Finish capturing the scene, post-process and render the effect, if any
	 */
	public void render()
	{
		captureEnd();

		if(effect != null)
		{
			effect.render( bufferScene );
		}
	}
}
