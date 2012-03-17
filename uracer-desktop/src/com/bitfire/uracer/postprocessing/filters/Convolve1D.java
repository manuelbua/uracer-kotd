package com.bitfire.uracer.postprocessing.filters;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.bitfire.uracer.postprocessing.IFilter;
import com.bitfire.uracer.utils.ShaderLoader;

public class Convolve1D extends Filter<Convolve1D>
{
	// TODO setParam for weights/offsets

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
		convolve1d = ShaderLoader.createShader( "convolve-1d", "convolve-1d", "#define LENGTH " + length );
		this.weights = weights;
		this.offsets = offsets;
	}

	public void dispose()
	{
		convolve1d.dispose();
		weights = offsets = null;
	}

	@Override
	public void upload()
	{
		convolve1d.begin();
		convolve1d.setUniform1fv( "SampleWeights", weights, 0, length );
		convolve1d.setUniform2fv( "SampleOffsets", offsets, 0, length * 2 /* libgdx asks for number of floats, not elements! */);
		convolve1d.setUniformi( "u_texture", u_texture_1 );
		convolve1d.end();
	}

	@Override
	protected void compute()
	{
		inputTexture.bind( u_texture_1 );
		convolve1d.begin();
		IFilter.quad.render( convolve1d );
		convolve1d.end();
	}
}