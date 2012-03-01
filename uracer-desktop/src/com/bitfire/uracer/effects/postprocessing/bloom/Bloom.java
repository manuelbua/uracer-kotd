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
import com.bitfire.uracer.utils.ShaderLoader;

public class Bloom implements IPostProcessorEffect
{
	public static boolean useAlphaChannelAsMask = false;

	/** how many blur pass */

	protected static ShaderProgram shThreshold;
	protected static ShaderProgram shBloom;
	protected static ShaderProgram shBlur;
	private static boolean shadersInitialized = false;

	private Color clearColor = Color.CLEAR;

	private FrameBuffer pingPongBuffer1;
	private FrameBuffer pingPongBuffer2;
	private Texture pingPongTex1;
	private Texture pingPongTex2;

	protected int blurPasses = 1;
	protected float bloomIntensity, bloomSaturation;
	protected float baseIntensity, baseSaturation;
	protected BloomSettings defaultSettings = new BloomSettings( "default", 2, 0.277f, 1f, .85f, 1.1f, .85f );

	protected float threshold;
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
		setSettings( defaultSettings );
	}

	protected void createShaders()
	{
		if(!Bloom.shadersInitialized)
		{
			Bloom.shadersInitialized = true;

			if(Bloom.useAlphaChannelAsMask)
				shThreshold = ShaderLoader.createShader( "bloom/screenspace", "bloom/masked-threshold" );
			else
				shThreshold = ShaderLoader.createShader( "bloom/screenspace", "bloom/threshold" );

			shBloom = ShaderLoader.createShader( "bloom/screenspace", "bloom/bloom" );
			shBlur = ShaderLoader.createShader( "bloom/blurspace", "bloom/gaussian" );
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

	public void setSettings(BloomSettings settings)
	{
		setThreshold( settings.bloomThreshold );
		setBaseIntesity( settings.baseIntensity );
		setBaseSaturation( settings.baseSaturation );
		setBloomIntesity( settings.bloomIntensity );
		setBloomSaturation( settings.bloomSaturation );
		setBlurPasses( settings.blurPasses );
	}

	public void setBlurPasses(int passes)
	{
		this.blurPasses = passes;
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

		setThreshold( threshold );
		setBaseIntesity( baseIntensity );
		setBaseSaturation( baseSaturation );
		setBloomIntesity( bloomIntensity );
		setBloomSaturation( bloomSaturation );

		pingPongTex1 = pingPongBuffer1.getColorBufferTexture();
		pingPongTex2 = pingPongBuffer2.getColorBufferTexture();
	}
}
