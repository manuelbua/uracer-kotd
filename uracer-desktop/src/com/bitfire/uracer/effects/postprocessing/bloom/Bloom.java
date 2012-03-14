package com.bitfire.uracer.effects.postprocessing.bloom;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.bitfire.uracer.effects.postprocessing.IPostProcessorEffect;
import com.bitfire.uracer.effects.postprocessing.PingPongBuffer;
import com.bitfire.uracer.effects.postprocessing.filters.Blur;
import com.bitfire.uracer.effects.postprocessing.filters.Blur.BlurType;
import com.bitfire.uracer.effects.postprocessing.filters.Combine;
import com.bitfire.uracer.effects.postprocessing.filters.Combine.Param;
import com.bitfire.uracer.effects.postprocessing.filters.Threshold;

public class Bloom implements IPostProcessorEffect
{
	public static boolean useAlphaChannelAsMask = false;

	private Color clearColor = Color.CLEAR;

	private PingPongBuffer pingPongBuffer;

	protected int blurPasses;
	protected float blurAmount;
	protected float bloomIntensity, bloomSaturation;
	protected float baseIntensity, baseSaturation;

	protected Blur blur;
	protected Threshold threshold;
	protected Combine combine;

	protected BlurType blurType;
	protected BloomSettings bloomSettings;

	protected boolean blending = false;

	public Bloom( int fboWidth, int fboHeight, Format frameBufferFormat )
	{
		pingPongBuffer = new PingPongBuffer( fboWidth, fboHeight, frameBufferFormat, false );

		blur = new Blur(fboWidth, fboHeight);
		threshold = new Threshold(Bloom.useAlphaChannelAsMask);
		combine = new Combine();

		BloomSettings s = new BloomSettings( "default", 2, 0.277f, 1f, .85f, 1.1f, .85f );
		setSettings(s);
	}

	@Override
	public void dispose()
	{
		combine.dispose();
		threshold.dispose();
		blur.dispose();
		pingPongBuffer.dispose();
	}

	public void setClearColor( float r, float g, float b, float a )
	{
		clearColor.set( r, g, b, a );
	}

	public void setBaseIntesity( float intensity )
	{
		combine.setParam( Param.Source1Intensity, intensity );
	}

	public void setBaseSaturation( float saturation )
	{
		combine.setParam( Param.Source1Saturation, saturation );
	}

	public void setBloomIntesity( float intensity )
	{
		combine.setParam( Param.Source2Intensity, intensity );
	}

	public void setBloomSaturation( float saturation )
	{
		combine.setParam( Param.Source2Saturation, saturation );

	}

	public void setThreshold( float gamma )
	{
		threshold.setTreshold( gamma );
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

		// setup threshold
		setThreshold( settings.bloomThreshold );

		// set combine
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

	@Override
	public Color getClearColor()
	{
		return clearColor;
	}

	@Override
	public void render( final Texture originalScene )
	{
		Gdx.gl.glDisable( GL10.GL_BLEND );
		Gdx.gl.glDisable( GL10.GL_DEPTH_TEST );
		Gdx.gl.glDepthMask( false );

		// cut bright areas of the picture and blit to smaller fbo
		threshold.render( originalScene, pingPongBuffer.getNextSourceBuffer() );

		// src in buffer1
		// result in buffer1
		blur.render( pingPongBuffer );
		pingPongBuffer.end();

		if( blending )
		{
			Gdx.gl.glEnable( GL10.GL_BLEND );
			Gdx.gl.glBlendFunc( GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA );
		}

		combine.render( originalScene, pingPongBuffer.getLastDestinationTexture() );
	}

	@Override
	public void resume()
	{
		blur.upload();
		threshold.upload();
		pingPongBuffer.rebind();
		setSettings( bloomSettings );
	}
}
