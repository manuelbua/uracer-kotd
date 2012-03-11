package com.bitfire.uracer.effects.postprocessing.filters;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.bitfire.uracer.utils.ShaderLoader;

public class Blur
{
	private enum Tap
	{
		Tap3x3(1),
		Tap5x5(2),
		Tap7x7(3);

		public final int length;
		public final int radius;
		public float[] weights;
		public float[] offsetsH;
		public float[] offsetsV;

		private Tap( int radius )
		{
			this.radius = radius;
			this.length = (radius*2)+1;

			convolveH = ShaderLoader.createShader("bloom/convolve-1d", "bloom/convolve-1d", "#define LENGTH " + length);
			convolveV = ShaderLoader.createShader("bloom/convolve-1d", "bloom/convolve-1d", "#define LENGTH " + length);

			weights = new float[length];
			offsetsH = new float[length*2];
			offsetsV = new float[length*2];
		}

		public void dispose()
		{
			convolveH.dispose();
			convolveV.dispose();
			weights = offsetsH = offsetsV = null;
		}

		public void bind()
		{
			convolveH.begin();
			convolveH.begin();
			convolveH.setUniform1fv( "SampleWeights", weights, 0, length );
			convolveH.setUniform2fv( "SampleOffsets", offsetsH, 0, length*2  /* libgdx ask for number of floats! */ );
			convolveH.end();

			convolveV.begin();
			convolveV.setUniform1fv( "SampleWeights", weights, 0, length );
			convolveV.setUniform2fv( "SampleOffsets", offsetsV, 0, length*2  /* libgdx ask for number of floats! */ );
			convolveV.end();
		}

		protected ShaderProgram convolveH = null, convolveV = null;
	}

	public enum BlurType
	{
		Gaussian3x3(Tap.Tap3x3),
		Gaussian5x5(Tap.Tap5x5),
		Gaussian5x5b(Tap.Tap5x5),	// R=9 (19x19)
		;

		public final Tap tap;
		private BlurType(Tap tapsize)
		{
			this.tap = tapsize;
		}
	}

	// blur
	private BlurType type;
	private float amount;
	private int passes;

	// fbo, textures
	private int fboWidth, fboHeight;
	private float invFboWidth, invFboHeight;
	private FrameBuffer pingPongBuffer1;
	private FrameBuffer pingPongBuffer2;
	private Texture pingPongTex1;
	private Texture pingPongTex2;

	public Blur( int fboWidth, int fboHeight, Format frameBufferFormat )
	{
		create( fboWidth, fboHeight, frameBufferFormat );
		setup();
	}

	private void create( int fboWidth, int fboHeight, Format frameBufferFormat )
	{
		// setup fbo
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
	}

	private void setup()
	{
		this.passes = 1;
		this.amount = 1f;
		setType(BlurType.Gaussian5x5);
	}

	public void dispose()
	{

		pingPongBuffer1.dispose();
		pingPongBuffer2.dispose();
	}

	public void rebind()
	{
		pingPongTex1 = pingPongBuffer1.getColorBufferTexture();
		pingPongTex2 = pingPongBuffer2.getColorBufferTexture();
		computeBlurWeightings();
	}

	public void setPasses(int passes)
	{
		this.passes = passes;
	}

	public void setType(BlurType type)
	{
		this.type = type;
		computeBlurWeightings();
	}

	// not all blur types support custom amounts at this time
	public void setAmount(float amount)
	{
		this.amount = amount;
		computeBlurWeightings();
	}

	public FrameBuffer getInputBuffer()
	{
		return pingPongBuffer1;
	}

	public FrameBuffer getOutputBuffer()
	{
		return pingPongBuffer1;
	}

	public void render( Mesh fullScreenQuad )
	{
		ShaderProgram h = this.type.tap.convolveH;
		ShaderProgram v = this.type.tap.convolveV;

		for( int i = 0; i < this.passes; i++ )
		{
			// horizontal pass
			pingPongTex1.bind( 0 );
			pingPongBuffer2.begin();
			{
				h.begin();
				{
					h.setUniformi( "u_texture", 0 );
					fullScreenQuad.render( h, GL20.GL_TRIANGLE_FAN, 0, 4 );
				}
				h.end();
			}
			pingPongBuffer2.end();

			// vertical pass
			pingPongTex2.bind( 0 );
			pingPongBuffer1.begin();
			{
				v.begin();
				{
					v.setUniformi( "u_texture", 0 );
					fullScreenQuad.render( v, GL20.GL_TRIANGLE_FAN, 0, 4 );
				}
				v.end();
			}
			pingPongBuffer1.end();
		}
	}

	private void computeBlurWeightings()
	{
		boolean hasdata = true;

		switch(this.type)
		{
		default:
			hasdata = false;
			break;

		case Gaussian3x3:
		case Gaussian5x5:
			computeKernel( this.type.tap.radius, this.amount, this.type.tap.weights );
			computeOffsets( this.type.tap.radius, this.invFboWidth, this.invFboHeight, this.type.tap.offsetsH, this.type.tap.offsetsV );
			break;

		case Gaussian5x5b:
			float[] outWeights = this.type.tap.weights;
			float[] outOffsetsH = this.type.tap.offsetsH;
			float[] outOffsetsV = this.type.tap.offsetsV;

			// weights and offsets are computed from a binomial distribution
			// and reduced to be used *only* with bilinearly-filtered texture lookups
			//
			// with radius = 2f
			// with rtt ratio = 0.25f

			float dx = this.invFboWidth;
			float dy = this.invFboHeight;

			// ---------------8<---------------8<---------------8<---------------8<---------------8<---------------8<
			// weights and offsets are computed from a binomial distribution
			// and reduced to be used *only* with bilinearly-filtered texture lookups
			//
			// with radius = 2f
			// with rtt ratio = 0.25f

			// weights
			outWeights[0] = 0.0702703f;
			outWeights[1] = 0.316216f;
			outWeights[2] = 0.227027f;
			outWeights[3] = 0.316216f;
			outWeights[4] = 0.0702703f;

			// horizontal offsets
			outOffsetsH[0] = -3.23077f;	outOffsetsH[1] = 0f;
			outOffsetsH[2] = -1.38462f;	outOffsetsH[3] = 0f;
			outOffsetsH[4] = 0f;		outOffsetsH[5] = 0f;
			outOffsetsH[6] = 1.38462f;	outOffsetsH[7] = 0f;
			outOffsetsH[8] = 3.23077f;	outOffsetsH[9] = 0f;

			// vertical offsets
			outOffsetsV[0] = 0f;	outOffsetsV[1] = -3.23077f;
			outOffsetsV[2] = 0f;	outOffsetsV[3] = -1.38462f;
			outOffsetsV[4] = 0f;	outOffsetsV[5] = 0f;
			outOffsetsV[6] = 0f;	outOffsetsV[7] = 1.38462f;
			outOffsetsV[8] = 0f;	outOffsetsV[9] = 3.23077f;

			// scale offsets from binomial space to screen space
			for(int i = 0; i < this.type.tap.length * 2; i++) {
				outOffsetsH[i] *= dx;
				outOffsetsV[i] *= dy;
			}
			// ---------------8<---------------8<---------------8<---------------8<---------------8<---------------8<

			break;
		}

		if(hasdata)
		{
			this.type.tap.bind();
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

	private void computeOffsets( int blurRadius, float dx, float dy, float[] outOffsetH, float[] outOffsetV )
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

}
