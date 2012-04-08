package com.bitfire.uracer.postprocessing.filters;

import com.bitfire.uracer.postprocessing.IFilter;
import com.bitfire.uracer.utils.ShaderLoader;

public class Convolve1D extends Filter<Convolve1D> {
	public enum Param implements Parameter {
		// @formatter:off
		Texture( "u_texture", 0 ),
		SampleWeights( "SampleWeights", 1 ),
		SampleOffsets( "SampleOffsets", 2 /* vec2 */ );
		// @formatter:on

		private String mnemonic;
		private int elementSize;

		private Param( String mnemonic, int arrayElementSize ) {
			this.mnemonic = mnemonic;
			this.elementSize = arrayElementSize;
		}

		@Override
		public String mnemonic() {
			return this.mnemonic;
		}

		@Override
		public int arrayElementSize() {
			return this.elementSize;
		}
	}

	public final int length;
	public float[] weights;
	public float[] offsets;

	public Convolve1D( int length ) {
		this( length, new float[ length ], new float[ length * 2 ] );
	}

	public Convolve1D( int length, float[] weights_data ) {
		this( length, weights_data, new float[ length * 2 ] );
	}

	public Convolve1D( int length, float[] weights_data, float[] offsets ) {
		super( ShaderLoader.fromFile( "convolve-1d", "convolve-1d", "#define LENGTH " + length ) );
		this.length = length;
		this.weights = weights_data;
		this.offsets = offsets;
	}

	@Override
	public void dispose() {
		super.dispose();
		weights = null;
		offsets = null;
	}

	@Override
	public void rebind() {
		setParams( Param.Texture, u_texture_1 );
		setParamsv( Param.SampleWeights, weights, 0, length );
		setParamsv( Param.SampleOffsets, offsets, 0, length * 2 /* libgdx asks for number of floats, not elements! */);
		endParams();
	}

	@Override
	protected void compute() {
		inputTexture.bind( u_texture_1 );
		program.begin();
		IFilter.quad.render( program );
		program.end();
	}
}