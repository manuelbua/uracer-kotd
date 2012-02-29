package com.bitfire.uracer.effects.postprocessing;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.bitfire.uracer.utils.ShaderLoader;

public class Bloom implements IPostProcessorEffect
{
	public static boolean useAlphaChannelAsMask = false;

	public enum ThresholdType { Luminance, Saturate, Test };
	public enum BloomMixing { WeightedAverage, Scaled, Test };

	private ThresholdType thresholdType = ThresholdType.Luminance;
	private BloomMixing bloomMixing = BloomMixing.WeightedAverage;

	/** how many blur pass */
	public int blurPasses = 1;

	protected static ShaderProgram shThresholdSat;
	protected static ShaderProgram shThresholdLum;
	protected static ShaderProgram shThresholdMaskedSat, shThresholdMaskedLum;
	protected static ShaderProgram shBloomScaled, shBloomWa;
	protected static ShaderProgram shBloomTest, shThresholdTest;
	protected static ShaderProgram shBlur;
	private static boolean shadersInitialized = false;

	// refs
	private ShaderProgram shThreshold, shBloom;

	private Color clearColor = Color.CLEAR;

	private FrameBuffer pingPongBuffer1;
	private FrameBuffer pingPongBuffer2;
	private Texture pingPongTex1;
	private Texture pingPongTex2;

	protected float bloomIntensity = 1.3f;
	protected float originalIntensity = 0.8f;
	protected float bloomSaturation = 1f;
	protected float originalSaturation = 1f;

	protected float treshold = 0.277f;
	protected boolean blending = false;
	private int w;
	private int h;

	public Bloom( int fboWidth, int fboHeight, Format frameBufferFormat )
	{
		pingPongBuffer1 = new FrameBuffer( frameBufferFormat, fboWidth, fboHeight, false );
		pingPongBuffer2 = new FrameBuffer( frameBufferFormat, fboWidth, fboHeight, false );

		pingPongTex1 = pingPongBuffer1.getColorBufferTexture();
		pingPongTex2 = pingPongBuffer2.getColorBufferTexture();

		createShaders();

		setSize( fboWidth, fboHeight );
	}

	protected void createShaders()
	{
		if(!Bloom.shadersInitialized)
		{
			Bloom.shadersInitialized = true;

			shBlur = ShaderLoader.createShader( "bloom/blurspace", "bloom/gaussian" );
			shBloomScaled = ShaderLoader.createShader( "bloom/screenspace", "bloom/bloom-scaled" );
			shBloomWa = ShaderLoader.createShader( "bloom/screenspace", "bloom/bloom-wa" );
			shThresholdSat = ShaderLoader.createShader( "bloom/screenspace", "bloom/treshold-sat" );
			shThresholdLum = ShaderLoader.createShader( "bloom/screenspace", "bloom/treshold-lum" );
			shThresholdMaskedSat = shThresholdMaskedLum = ShaderLoader.createShader( "bloom/screenspace", "bloom/maskedtreshold-sat" ); // TODO

			shBloomTest = ShaderLoader.createShader( "bloom/screenspace", "bloom/bloom-test" );
			shThresholdTest = ShaderLoader.createShader( "bloom/screenspace", "bloom/threshold-test" );
		}

		setThresholdType( thresholdType );
		setBloomMixing( bloomMixing );
	}

	public void setThresholdType(ThresholdType threshold)
	{
		thresholdType = threshold;

		switch( threshold )
		{
		case Saturate:
			if(Bloom.useAlphaChannelAsMask)
				shThreshold = shThresholdMaskedSat;
			else
				shThreshold = shThresholdSat;
			break;

		case Test:
			shThreshold = shThresholdTest;
			break;

		default:
		case Luminance:
			if(Bloom.useAlphaChannelAsMask)
				shThreshold = shThresholdMaskedLum;	// TODO
			else
				shThreshold = shThresholdLum;
		}

		setTreshold( treshold );
	}

	public void setBloomMixing(BloomMixing mixing)
	{
		bloomMixing = mixing;

		switch( mixing )
		{
		case Scaled:
			shBloom = shBloomScaled;
			break;

		case Test:
			shBloom = shBloomTest;
			break;

		default:
		case WeightedAverage:
			shBloom = shBloomWa;
			break;
		}

		setBloomIntesity( bloomIntensity );
		setOriginalIntesity( originalIntensity );

		setBloomSaturation( bloomSaturation );
		setOriginalSaturation( originalSaturation );
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

	public void setOriginalIntesity( float intensity )
	{
		originalIntensity = intensity;
		shBloom.begin();
		{
			shBloom.setUniformf( "OriginalIntensity", intensity );
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

	public void setOriginalSaturation( float saturation )
	{
		originalSaturation = saturation;
		shBloom.begin();
		{
			shBloom.setUniformf( "OriginalSaturation", saturation );
		}
		shBloom.end();
	}

	public void setTreshold( float treshold )
	{
		this.treshold = treshold;
		shThreshold.begin();
		{
			shThreshold.setUniformf( "treshold", treshold );

			if(thresholdType == ThresholdType.Saturate)
			{
				shThreshold.setUniformf( "tresholdD", (1f / treshold) );
			}

			if(thresholdType == ThresholdType.Test)
			{
				shThreshold.setUniformf( "tresholdInvTx", (1f / (1f-treshold)) );	// correct
//				shThreshold.setUniformf( "tresholdInvTx", (1f / (treshold)) );		// but does it looks better?
			}
		}
		shThreshold.end();
	}

	public void setBlending(boolean blending)
	{
		this.blending = blending;
	}

	private void setSize( int FBO_W, int FBO_H )
	{
		w = FBO_W;
		h = FBO_H;
		shBlur.begin();
		shBlur.setUniformf( "size", FBO_W, FBO_H );
		shBlur.end();
	}

	/**
	 * Call this when application is exiting.
	 *
	 */
	@Override
	public void dispose()
	{
		pingPongBuffer1.dispose();
		pingPongBuffer2.dispose();

		shBlur.dispose();
		shBloomScaled.dispose();
		shBloomWa.dispose();
		shThresholdSat.dispose();
		shThresholdLum.dispose();
		shThresholdMaskedSat.dispose();
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

		gaussianBlur( fullScreenQuad, originalScene );

		if( blending )
		{
			Gdx.gl.glEnable( GL10.GL_BLEND );
			Gdx.gl.glBlendFunc( GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA );
		}

		originalScene.bind( 0 );
		pingPongTex1.bind( 1 );
		shBloom.begin();
		{
			shBloom.setUniformi( "u_texture0", 0 );
			shBloom.setUniformi( "u_texture1", 1 );
			fullScreenQuad.render( shBloom, GL20.GL_TRIANGLE_FAN );
		}
		shBloom.end();
	}

	private void gaussianBlur( Mesh fullScreenQuad, Texture originalScene )
	{
		// cut bright areas of the picture and blit to smaller fbo

		originalScene.bind( 0 );
		pingPongBuffer1.begin();
		{
			shThreshold.begin();
			{
				shThreshold.setUniformi( "u_texture0", 0 );
				fullScreenQuad.render( shThreshold, GL20.GL_TRIANGLE_FAN, 0, 4 );
			}
			shThreshold.end();
		}
		pingPongBuffer1.end();

		for( int i = 0; i < blurPasses; i++ )
		{
			pingPongTex1.bind( 0 );

			// horizontal
			pingPongBuffer2.begin();
			{
				shBlur.begin();
				{
					shBlur.setUniformi( "u_texture", 0 );
					shBlur.setUniformf( "dir", 1f, 0f );
					fullScreenQuad.render( shBlur, GL20.GL_TRIANGLE_FAN, 0, 4 );
				}
				shBlur.end();
			}
			pingPongBuffer2.end();

			pingPongTex2.bind( 0 );
			// vertical
			pingPongBuffer1.begin();
			{
				shBlur.begin();
				{
					shBlur.setUniformi( "u_texture", 0 );
					shBlur.setUniformf( "dir", 0f, 1f );

					fullScreenQuad.render( shBlur, GL20.GL_TRIANGLE_FAN, 0, 4 );
				}
				shBlur.end();
			}
			pingPongBuffer1.end();
		}
	}

	@Override
	public void resume()
	{
		setSize( w, h );
		setTreshold( treshold );
		setBloomIntesity( bloomIntensity );
		setOriginalIntesity( originalIntensity );

		pingPongTex1 = pingPongBuffer1.getColorBufferTexture();
		pingPongTex2 = pingPongBuffer2.getColorBufferTexture();
	}
}
