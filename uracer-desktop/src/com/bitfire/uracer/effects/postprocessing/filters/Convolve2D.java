package com.bitfire.uracer.effects.postprocessing.filters;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.bitfire.uracer.effects.postprocessing.FullscreenQuad;

public class Convolve2D
{
	public final int radius;
	public final int length;	// NxN taps filter, w/ N=length

	public final float[] weights, offsetsHor, offsetsVert;

	private Convolve1D hor, vert;
	private ShaderProgram convh, convv;
	public Convolve2D(int radius)
	{
		this.radius = radius;
		length = (radius*2)+1;

		hor = new Convolve1D( length );
		vert = new Convolve1D( length, hor.weights );

		weights = hor.weights;
		offsetsHor = hor.offsets;
		offsetsVert = vert.offsets;

		convh = hor.convolve1d;
		convv = vert.convolve1d;
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

	public void renderHorizontal(FullscreenQuad quad)
	{
		convh.begin();
		{
			convh.setUniformi( "u_texture", 0 );
			quad.render( convh );
		}
		convh.end();
	}

	public void renderVertical(FullscreenQuad quad)
	{
		convv.begin();
		{
			convv.setUniformi( "u_texture", 0 );
			quad.render( convv );
		}
		convv.end();
	}
}
