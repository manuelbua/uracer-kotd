package com.bitfire.uracer.effects.postprocessing.filters;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.bitfire.uracer.effects.postprocessing.FullscreenQuad;
import com.bitfire.uracer.utils.ShaderLoader;

public class Convolve1D
{
	public final int length;
	public float[] weights;
	public float[] offsets;

	protected ShaderProgram convolve1d;

	public Convolve1D( int length )
	{
		this( length, new float[ length ], new float[ length * 2 ] );
	}

	public Convolve1D( int length, float[] weights )
	{
		this( length, weights, new float[ length * 2 ] );
	}

	public Convolve1D( int length, float[] weights, float[] offsets )
	{
		this.length = length;
		convolve1d = ShaderLoader.createShader( "bloom/convolve-1d", "bloom/convolve-1d", "#define LENGTH " + length );
		this.weights = weights;
		this.offsets = offsets;
	}

	public void dispose()
	{
		convolve1d.dispose();
		weights = offsets = null;
	}

	public void upload()
	{
		convolve1d.begin();
		convolve1d.setUniform1fv( "SampleWeights", weights, 0, length );
		convolve1d.setUniform2fv( "SampleOffsets", offsets, 0, length * 2 /* libgdx ask for number of floats! */);
		convolve1d.end();
	}

	// public void render(FullscreenQuad quad, Texture source, FrameBuffer dest)
	public void render( FullscreenQuad quad, Texture source )
	{
		source.bind( 0 );
		convolve1d.begin();
		{
			convolve1d.setUniformi( "u_texture", 0 );
			quad.render( convolve1d );
		}
		convolve1d.end();
	}
}