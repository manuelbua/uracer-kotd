package com.bitfire.uracer.effects.postprocessing.bloom;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.bitfire.uracer.effects.postprocessing.IPostProcessorEffect;
import com.bitfire.uracer.effects.postprocessing.filters.Blur;
import com.bitfire.uracer.effects.postprocessing.filters.Blur.BlurType;
import com.bitfire.uracer.utils.ShaderLoader;

public class Bloom implements IPostProcessorEffect
{
	public static boolean useAlphaChannelAsMask = false;

	protected static ShaderProgram shThreshold, shBloom;
	private static boolean shadersInitialized = false;


	private Color clearColor = Color.CLEAR;

	private FrameBuffer blurInput, blurOutput;
	private Texture blurInputTex, blurOutputTex;

	protected int blurPasses;
	protected float blurAmount;
	protected float bloomIntensity, bloomSaturation;
	protected float baseIntensity, baseSaturation;
	protected Blur blur;
	protected BlurType blurType;
	protected BloomSettings bloomSettings;

	protected float threshold;
	protected boolean blending = false;

	public Bloom( int fboWidth, int fboHeight, Format frameBufferFormat )
	{
		blur = new Blur(fboWidth, fboHeight, frameBufferFormat);

		blurInput = blur.getInputBuffer();
		blurOutput = blur.getOutputBuffer();

		blurInputTex = blur.getInputBuffer().getColorBufferTexture();
		blurOutputTex = blur.getOutputBuffer().getColorBufferTexture();

		BloomSettings s = new BloomSettings( "default", 2, 0.277f, 1f, .85f, 1.1f, .85f );
		createShaders(s);
		setSettings(s);
	}

	protected void createShaders(BloomSettings settings)
	{
		if(!Bloom.shadersInitialized)
		{
			Bloom.shadersInitialized = true;

			shBloom = ShaderLoader.createShader( "bloom/screenspace", "bloom/bloom" );

			if(Bloom.useAlphaChannelAsMask)
				shThreshold = ShaderLoader.createShader( "bloom/screenspace", "bloom/masked-threshold" );
			else
				shThreshold = ShaderLoader.createShader( "bloom/screenspace", "bloom/threshold" );
		}
	}

	public void setClearColor( float r, float g, float b, float a )
	{
		clearColor.set( r, g, b, a );
	}

	public void setBloomIntesity( float intensity )
	{
		bloomIntensity = intensity;
		shBloom.begin();
		{
			shBloom.setUniformf( "BloomIntensity", intensity );
		}
		shBloom.end();
	}

	public void setBaseIntesity( float intensity )
	{
		baseIntensity = intensity;
		shBloom.begin();
		{
			shBloom.setUniformf( "BaseIntensity", intensity );
		}
		shBloom.end();
	}

	public void setBloomSaturation( float saturation )
	{
		bloomSaturation = saturation;
		shBloom.begin();
		{
			shBloom.setUniformf( "BloomSaturation", saturation );
		}
		shBloom.end();
	}

	public void setBaseSaturation( float saturation )
	{
		baseSaturation = saturation;
		shBloom.begin();
		{
			shBloom.setUniformf( "BaseSaturation", saturation );
		}
		shBloom.end();
	}

	public void setThreshold( float treshold )
	{
		this.threshold = treshold;
		shThreshold.begin();
		{
			shThreshold.setUniformf( "treshold", treshold );
			shThreshold.setUniformf( "tresholdInvTx", (1f / (1f-treshold)) );	// correct
//			shThreshold.setUniformf( "tresholdInvTx", (1f / (treshold)) );		// does this look better?
		}
		shThreshold.end();
	}

	public void setBlending(boolean blending)
	{
		this.blending = blending;
	}

	public void setBlurType(BlurType type)
	{
		blur.setType( type );
	}

	public void setSettings(BloomSettings settings)
	{
		this.bloomSettings = settings;

		// setup bloom
		setThreshold( settings.bloomThreshold );
		setBaseIntesity( settings.baseIntensity );
		setBaseSaturation( settings.baseSaturation );
		setBloomIntesity( settings.bloomIntensity );
		setBloomSaturation( settings.bloomSaturation );

		// setup blur
		setBlurPasses( settings.blurPasses );
		setBlurAmount( settings.blurAmount );
		setBlurType( settings.blurType );
	}

	public void setBlurPasses(int passes)
	{
		blur.setPasses( passes );
	}

	public void setBlurAmount(float amount)
	{
		blur.setAmount( amount );
	}


	/**
	 * Call this when application is exiting.
	 *
	 */

	@Override
	public void dispose()
	{
		blur.dispose();
		shBloom.dispose();
		shThreshold.dispose();
	}

	@Override
	public Color getClearColor()
	{
		return clearColor;
	}

	@Override
	public void render( Mesh fullScreenQuad, Texture originalScene )
	{
		Gdx.gl.glDisable( GL10.GL_BLEND );
		Gdx.gl.glDisable( GL10.GL_DEPTH_TEST );
		Gdx.gl.glDepthMask( false );

		thresholdAndBlur( fullScreenQuad, originalScene );

		if( blending )
		{
			Gdx.gl.glEnable( GL10.GL_BLEND );
			Gdx.gl.glBlendFunc( GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA );
		}

		originalScene.bind( 0 );
		blurOutputTex.bind( 1 );
		shBloom.begin();
		{
			shBloom.setUniformi( "u_texture0", 0 );
			shBloom.setUniformi( "u_texture1", 1 );
			fullScreenQuad.render( shBloom, GL20.GL_TRIANGLE_FAN );
		}
		shBloom.end();
	}

	private void thresholdAndBlur( Mesh fullScreenQuad, Texture originalScene )
	{
		// cut bright areas of the picture and blit to smaller fbo
		originalScene.bind( 0 );
		blur.getInputBuffer().begin();
		{
			shThreshold.begin();
			{
				shThreshold.setUniformi( "u_texture0", 0 );
				fullScreenQuad.render( shThreshold, GL20.GL_TRIANGLE_FAN, 0, 4 );
			}
			shThreshold.end();
		}
		blur.getInputBuffer().end();

		blur.render( fullScreenQuad );
	}

	@Override
	public void resume()
	{
		blur.rebind();
		setSettings( bloomSettings );
		blurInputTex = blurInput.getColorBufferTexture();
		blurOutputTex = blurOutput.getColorBufferTexture();
	}
}
