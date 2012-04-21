package com.bitfire.uracer.postprocessing.filters;

import com.bitfire.uracer.postprocessing.IFilter;
import com.bitfire.uracer.utils.ShaderLoader;

public final class Vignetting extends Filter<Vignetting> {

	private float x, y;
	private float intensity;

	public enum Param implements Parameter {
		// @formatter:off
		Texture1("u_texture0",0),
		VignetteIntensity("VignetteIntensity",0),
		VignetteX("VignetteX",0),
		VignetteY("VignetteY",0);
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

	public Vignetting() {
		super( ShaderLoader.fromFile( "screenspace", "vignetting" ) );
		rebind();
	}

	public void setIntensity( float intensity ) {
		this.intensity = intensity;
		setParam( Param.VignetteIntensity, intensity );
	}

	public void setCoords( float x, float y ) {
		this.x = x;
		this.y = y;
		setParams( Param.VignetteX, x );
		setParams( Param.VignetteY, y );
		endParams();
	}

	public void setX( float x ) {
		this.x = x;
		setParam( Param.VignetteX, x );
	}

	public void setY( float y ) {
		this.y = y;
		setParam( Param.VignetteY, y );
	}

	@Override
	public void rebind() {
		setParams( Param.Texture1, u_texture_1 );
//		setParams( Param.VignetteIntensity, intensity );
		setParams( Param.VignetteX, x );
		setParams( Param.VignetteY, y );
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
