package com.bitfire.uracer.effects.postprocessing.filters;

import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.IntMap;
import com.bitfire.uracer.effects.postprocessing.FullscreenQuad;
import com.bitfire.uracer.effects.postprocessing.PingPongBuffer;

public class Blur
{
	private enum Tap
	{
		Tap3x3(1),
		Tap5x5(2),
		Tap7x7(3);

		public final int radius;

		private Tap( int radius )
		{
			this.radius = radius;
		}
	}

	public enum BlurType
	{
		Gaussian3x3(Tap.Tap3x3),
		Gaussian5x5(Tap.Tap5x5),
		Gaussian5x5b(Tap.Tap5x5),	// R=9 (19x19)
		;

		public final Tap tap;
		private BlurType(Tap tap)
		{
			this.tap = tap;
		}
	}

	// blur
	private BlurType type;
	private float amount;
	private int passes;

	// fbo, textures
	private PingPongBuffer ppBuffer;
	private float invFboWidth, invFboHeight;
	private IntMap<Convolve2D> convolve = new IntMap<Convolve2D>(Tap.values().length);

	public Blur( int fboWidth, int fboHeight, Format frameBufferFormat )
	{
		create( fboWidth, fboHeight, frameBufferFormat );
		setup();
	}

	private void create( int fboWidth, int fboHeight, Format frameBufferFormat )
	{
		// precompute constants
		this.invFboWidth = 1f / (float)fboWidth;
		this.invFboHeight = 1f / (float)fboHeight;

		ppBuffer = new PingPongBuffer( fboWidth, fboHeight, frameBufferFormat, false );

		for(Tap tap : Tap.values())
			convolve.put( tap.radius, new Convolve2D(tap.radius) );
	}

	private void setup()
	{
		this.passes = 1;
		this.amount = 1f;
		setType(BlurType.Gaussian5x5);
	}

	public void dispose()
	{
		for(Convolve2D c : convolve.values())
			c.dispose();

		ppBuffer.dispose();
	}

	public void rebind()
	{
		ppBuffer.rebind();
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
		return ppBuffer.buffer1;
	}

	public FrameBuffer getOutputBuffer()
	{
		return ppBuffer.buffer1;
	}

	public void render( FullscreenQuad quad )
	{
		Convolve2D c = convolve.get( this.type.tap.radius );

		for( int i = 0; i < this.passes; i++ )
		{
			// horizontal pass
			ppBuffer.begin();
			{
				c.renderHorizontal(quad);
			}

			// vertical pass
			ppBuffer.swap();
			{
				c.renderVertical(quad);
			}
			ppBuffer.end();
		}
	}

	private void computeBlurWeightings()
	{
		boolean hasdata = true;
		Convolve2D c = convolve.get( this.type.tap.radius );

		float[] outWeights = c.weights;
		float[] outOffsetsH = c.offsetsHor;
		float[] outOffsetsV = c.offsetsVert;

		switch(this.type)
		{
		default:
			hasdata = false;
			break;

		case Gaussian3x3:
		case Gaussian5x5:
			computeKernel( this.type.tap.radius, this.amount, outWeights );
			computeOffsets( this.type.tap.radius, this.invFboWidth, this.invFboHeight, outOffsetsH, outOffsetsV );
			break;

		case Gaussian5x5b:

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
			for(int i = 0; i < c.length * 2; i++) {
				outOffsetsH[i] *= dx;
				outOffsetsV[i] *= dy;
			}
			// ---------------8<---------------8<---------------8<---------------8<---------------8<---------------8<

			break;
		}

		if(hasdata)
		{
			c.upload();
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
