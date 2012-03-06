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
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.effects.postprocessing.IPostProcessorEffect;
import com.bitfire.uracer.effects.postprocessing.bloom.BloomSettings.BlurType;
import com.bitfire.uracer.utils.ShaderLoader;

public class Bloom implements IPostProcessorEffect
{
	public static boolean useAlphaChannelAsMask = false;

	/** how many blur pass */

	protected static ShaderProgram shThreshold;
	protected static ShaderProgram shBloom;
	protected static ShaderProgram shBlur, shBlurSimple;
	private static boolean shadersInitialized = false;

	// blur
	private float[] blurSampleWeights = null;
	private float[] blurSampleOffsetsH = null;
	private float[] blurSampleOffsetsV = null;
	private final int BlurRadius = 2;
	private final int BlurKernelSize = (BlurRadius * 2) + 1;

	private Color clearColor = Color.CLEAR;

	private FrameBuffer pingPongBuffer1;
	private FrameBuffer pingPongBuffer2;
	private Texture pingPongTex1;
	private Texture pingPongTex2;

	protected int blurPasses;
	protected float blurAmount;
	protected float bloomIntensity, bloomSaturation;
	protected float baseIntensity, baseSaturation;
	protected BlurType blurType;
	protected BloomSettings bloomSettings;

	protected float threshold;
	protected boolean blending = false;
	private int fboWidth, fboHeight;
	private float invFboWidth, invFboHeight;

	public Bloom( int fboWidth, int fboHeight, Format frameBufferFormat )
	{
		this.fboWidth = fboWidth;
		this.fboHeight = fboHeight;
		this.invFboWidth = 1f / (float)this.fboWidth;
		this.invFboHeight = 1f / (float)this.fboHeight;

		pingPongBuffer1 = new FrameBuffer( frameBufferFormat, fboWidth, fboHeight, false );
		pingPongBuffer2 = new FrameBuffer( frameBufferFormat, fboWidth, fboHeight, false );

		pingPongTex1 = pingPongBuffer1.getColorBufferTexture();
		pingPongTex2 = pingPongBuffer2.getColorBufferTexture();

//		pingPongTex1.setFilter( TextureFilter.Nearest, TextureFilter.Nearest );
//		pingPongTex2.setFilter( TextureFilter.Nearest, TextureFilter.Nearest );

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

			shBlurSimple = ShaderLoader.createShader( "bloom/blur-simple", "bloom/blur-simple" );
			shBlur = ShaderLoader.createShader( "bloom/blur", "bloom/blur" );
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

	public void setBlurType(BlurType blurType)
	{
		this.blurType = blurType;

		if( this.blurType == BlurType.Gaussian || this.blurType == BlurType.GaussianBilinear )
			computeBlurWeightings( this.blurAmount );
	}

	public void setSettings(BloomSettings settings)
	{
		this.bloomSettings = settings;

		setThreshold( settings.bloomThreshold );
		setBaseIntesity( settings.baseIntensity );
		setBaseSaturation( settings.baseSaturation );
		setBloomIntesity( settings.bloomIntensity );
		setBloomSaturation( settings.bloomSaturation );

		setBlurPasses( settings.blurPasses );

		// avoid computing blur weights 2x
		setBlurType( BlurType.GaussianApproximation );
		setBlurAmount( settings.blurAmount );
		setBlurType( settings.blurType );
		setBlurSize();
	}

	public void setBlurPasses(int passes)
	{
		this.blurPasses = passes;
	}

	public void setBlurAmount(float amount)
	{
		this.blurAmount = amount;

		if( this.blurType == BlurType.Gaussian || this.blurType == BlurType.GaussianBilinear )
			computeBlurWeightings( this.blurAmount );
	}

	private void computeBlurWeightings( float blurAmount )
	{
		// need memory?
		if(blurSampleWeights == null) blurSampleWeights = new float[BlurKernelSize];
		if(blurSampleOffsetsH == null) blurSampleOffsetsH = new float[BlurKernelSize * 2];	// x-y pairs
		if(blurSampleOffsetsV == null) blurSampleOffsetsV = new float[BlurKernelSize * 2];	// x-y pairs

		switch(this.blurType)
		{
		case GaussianBilinear:
			// opt gaussian (exploit bilinear filtering on texture units, good on mid-to-big-sized FBOs, ie. rtt>~0.3)
			computeGaussianViaBilinear( this.invFboWidth, 0, blurAmount, BlurKernelSize, blurSampleWeights, blurSampleOffsetsH );
			computeGaussianViaBilinear( 0, this.invFboHeight, blurAmount, BlurKernelSize, blurSampleWeights, blurSampleOffsetsV );
			break;

		case Gaussian:
			// gaussian (good on small-sizes FBOs, ie. rtt<=0.2)
			computeKernel( BlurRadius, blurAmount, blurSampleWeights );
			computeOffsets( BlurRadius, this.invFboWidth, this.invFboHeight, blurSampleOffsetsH, blurSampleOffsetsV );
			break;
		}
	}

	private void computeGaussianViaBilinear( float dx, float dy, float blurAmount, int sampleCount, float[] outWeights, float[] outOffsets )
	{
		final int X = 0, Y = 1;
		int halfSampleCount = sampleCount / 2;

		outWeights[0] = computeGaussian( blurAmount, 0 );
		outOffsets[0+X] = 0;
		outOffsets[0+Y] = 0;

		float totalWeights = outWeights[0];
		Vector2 delta = new Vector2();
		for(int i = 0, j = 0; i < halfSampleCount; i++, j+=2)
		{
			float weight = computeGaussian( blurAmount, i+1 );

			outWeights[i * 2 + 1] = weight;
			outWeights[i * 2 + 2] = weight;

			totalWeights += weight * 2;

			// To get the maximum amount of blurring from a limited number of
			// pixel shader samples, we take advantage of the bilinear filtering
			// hardware inside the texture fetch unit. If we position our texture
			// coordinates exactly halfway between two texels, the filtering unit
			// will average them for us, giving two samples for the price of one.
			// This allows us to step in units of two texels per sample, rather
			// than just one at a time. The 1.5 offset kicks things off by
			// positioning us nicely in between two texels.
			float sampleOffset = i * 2 + 1.5f;

			delta.set( dx, dy ).mul(sampleOffset);

			// Store texture coordinate offsets for the positive and negative taps.
			outOffsets[i * 2 + j + 2 + X] = delta.x;
			outOffsets[i * 2 + j + 2 + Y] = delta.y;
			outOffsets[i * 2 + j + 4 + X] = -delta.x;
			outOffsets[i * 2 + j + 4 + Y] = -delta.y;
		}

		// Normalize the list of sample weightings, so they will always sum to one.
		for( int i = 0; i < sampleCount; i++ )
		{
			outWeights[i] /= totalWeights;
		}
	}

	private void computeKernel( int blurRadius, float blurAmount, float[] outKernel )
	{
		int radius = blurRadius;
		float amount = blurAmount;

//		float sigma = (float)radius / amount;
		float sigma = amount;

		float twoSigmaSquare = 2.0f * sigma * sigma;
		float sigmaRoot = (float)Math.sqrt( twoSigmaSquare * Math.PI );
		float total = 0.0f;
		float distance = 0.0f;
		int index = 0;

		for( int i = -radius; i <= radius; ++i )
		{
			distance = i * i;
			index = i + radius;
			outKernel[index] = (float)Math.exp( -distance / twoSigmaSquare ) / sigmaRoot;
			total += outKernel[index];
		}

		int size = (radius*2)+1;
		for( int i = 0; i < size; ++i )
			outKernel[i] /= total;
	}

	public void computeOffsets( int blurRadius, float dx, float dy, float[] outOffsetH, float[] outOffsetV )
	{
		int radius = blurRadius;

		final int X = 0, Y = 1;
		for( int i = -radius, j = 0; i <= radius; ++i, j+=2 )
		{
			outOffsetH[j + X] = i * dx;
			outOffsetH[j + Y] = 0;

			outOffsetV[j + X] = 0;
			outOffsetV[j + Y] = i * dy;
		}
	}

	/**
	 * Evaluates a single point on the gaussian falloff curve. Used for setting up the blur filter weightings.
	 */
	private float computeGaussian( float blurAmount, float n )
	{
		float theta = blurAmount;
		return (float)((1.0 / Math.sqrt( 2 * Math.PI * theta )) * Math.exp( -(n * n) / (2 * theta * theta) ));
	}

	private void setBlurSize()
	{
		if(this.blurType == BlurType.GaussianApproximation)
		{
			shBlurSimple.begin();
			shBlurSimple.setUniformf( "size", this.fboWidth, this.fboHeight );
			shBlurSimple.end();
		}
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

		shBlurSimple.dispose();
		shBlur.dispose();
		shBloom.dispose();
		shThreshold.dispose();

		blurSampleWeights = null;
		blurSampleOffsetsH = null;
		blurSampleOffsetsV = null;
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

		renderGaussianBlur( fullScreenQuad, originalScene );

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

	private void renderGaussianBlur( Mesh fullScreenQuad, Texture originalScene )
	{
		// cut bright areas of the picture and blit to smaller fbo

		boolean isSimpleBlur = (this.blurType == BlurType.GaussianApproximation);
		ShaderProgram blur = (isSimpleBlur ? shBlurSimple : shBlur);

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
			// horizontal
			pingPongTex1.bind( 0 );
			pingPongBuffer2.begin();
			{
				blur.begin();
				{
					blur.setUniformi( "u_texture", 0 );

					if(isSimpleBlur)
						blur.setUniformf( "dir", 1f, 0f );
					else
					{
						blur.setUniform1fv( "SampleWeights", blurSampleWeights, 0, BlurKernelSize );
						blur.setUniform2fv( "SampleOffsets", blurSampleOffsetsH, 0, BlurKernelSize*2  /* libgdx ask for number of floats! */ );
					}

					fullScreenQuad.render( blur, GL20.GL_TRIANGLE_FAN, 0, 4 );
				}
				blur.end();
			}
			pingPongBuffer2.end();

			// vertical
			pingPongTex2.bind( 0 );
			pingPongBuffer1.begin();
			{
				blur.begin();
				{
					blur.setUniformi( "u_texture", 0 );

					if(isSimpleBlur)
						blur.setUniformf( "dir", 0f, 1f );
					else
					{
						blur.setUniform1fv( "SampleWeights", blurSampleWeights, 0, BlurKernelSize );
						blur.setUniform2fv( "SampleOffsets", blurSampleOffsetsV, 0, BlurKernelSize*2 /* libgdx ask for number of floats! */ );
					}

					fullScreenQuad.render( blur, GL20.GL_TRIANGLE_FAN, 0, 4 );
				}
				blur.end();
			}
			pingPongBuffer1.end();
		}
	}

	@Override
	public void resume()
	{
		setSettings( bloomSettings );
		pingPongTex1 = pingPongBuffer1.getColorBufferTexture();
		pingPongTex2 = pingPongBuffer2.getColorBufferTexture();
	}
}
