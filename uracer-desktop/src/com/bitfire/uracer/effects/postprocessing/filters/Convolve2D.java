package com.bitfire.uracer.effects.postprocessing.filters;

import com.bitfire.uracer.effects.postprocessing.PingPongBuffer;

public class Convolve2D
{
	public final int radius;
	public final int length; // NxN taps filter, w/ N=length

	public final float[] weights, offsetsHor, offsetsVert;

	private Convolve1D hor, vert;

	public Convolve2D( int radius )
	{
		this.radius = radius;
		length = (radius * 2) + 1;

		hor = new Convolve1D( length );
		vert = new Convolve1D( length, hor.weights );

		weights = hor.weights;
		offsetsHor = hor.offsets;
		offsetsVert = vert.offsets;
	}

	public void dispose()
	{
		hor.dispose();
		vert.dispose();
	}

	public void upload()
	{
		hor.upload();
		vert.upload();
	}

	/**
	 * Expects input to be in PingPongBuffer.buffer1
	 */

	// public void render(FullscreenQuad quad, Texture source, FrameBuffer dest)
	public void render( PingPongBuffer buffer )
	{
		hor.render( buffer.pingPong() );
		vert.render( buffer.pingPong() );

//		Texture src = buffer.getNextSourceTexture(); buffer.pingPong();
//		hor.render( quad, src );
//		src = buffer.getNextSourceTexture(); buffer.pingPong();
//		vert.render( quad, src );
	}
}
