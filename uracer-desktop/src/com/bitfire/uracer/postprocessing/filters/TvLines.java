package com.bitfire.uracer.postprocessing.filters;

import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.bitfire.uracer.postprocessing.IFilter;
import com.bitfire.uracer.utils.ShaderLoader;

public final class TvLines extends Filter<TvLines> {
	private float time, offset;
	private float[] resolution;

	public enum Param implements Parameter {
		// @formatter:off
		Texture0("u_texture0",0),
		Time("time",0),
		Resolution("resolution",2),
		Offset("offset",0)
		;
		// @formatter:on

		private final String mnemonic;
		private int elementSize;

		private Param( String m, int elementSize ) {
			this.mnemonic = m;
			this.elementSize = elementSize;
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

	public TvLines() {
		super( ShaderLoader.fromFile( "screenspace", "tv-lines" ) );
		time = 0;
		resolution = new float[ 2 ];

		rebind();
	}

	public void setTime( float time ) {
		this.time = time;
		setParam( Param.Time, time );
	}

	public void setResolution( float width, float height ) {
		resolution[0] = width;
		resolution[1] = height;
		setParamsv( Param.Resolution, resolution, 0, 2 );
	}

	public void setOffset( float offset ) {
		this.offset = offset;
		setParam(Param.Offset, this.offset );
	}
	@Override
	public void rebind() {
		setParams( Param.Texture0, u_texture0 );
		setParams( Param.Time, time );
		setParamsv( Param.Resolution, resolution, 0, 2 );
		endParams();
	}

	@Override
	protected void compute() {
		TextureWrap u = inputTexture.getUWrap();
		TextureWrap v = inputTexture.getVWrap();
		inputTexture.setWrap( TextureWrap.Repeat, TextureWrap.Repeat );
//		inputTexture.setFilter( TextureFilter.Linear, TextureFilter.Linear );

		inputTexture.bind( u_texture0 );

		program.begin();
		IFilter.quad.render( program );
		program.end();

		inputTexture.setWrap( u, v );
	}
}
