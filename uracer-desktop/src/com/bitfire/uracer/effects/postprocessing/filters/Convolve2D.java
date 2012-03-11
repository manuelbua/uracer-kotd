package com.bitfire.uracer.effects.postprocessing.filters;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

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

	public void renderHorizontal(Mesh fullScreenQuad)
	{
		convh.begin();
		{
			convh.setUniformi( "u_texture", 0 );
			fullScreenQuad.render( convh, GL20.GL_TRIANGLE_FAN, 0, 4 );
		}
		convh.end();
	}

	public void renderVertical(Mesh fullScreenQuad)
	{
		convv.begin();
		{
			convv.setUniformi( "u_texture", 0 );
			fullScreenQuad.render( convv, GL20.GL_TRIANGLE_FAN, 0, 4 );
		}
		convv.end();
	}
}
