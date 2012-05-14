package com.bitfire.uracer.postprocessing.filters;

import com.bitfire.uracer.postprocessing.IFilter;
import com.bitfire.uracer.utils.ShaderLoader;

public final class Vignetting extends Filter<Vignetting> {

	private float x, y;
	private float intensity, saturation, saturationMul;

	public enum Param implements Parameter {
		// @formatter:off
		Texture1("u_texture0",0),
		VignetteIntensity("VignetteIntensity",0),
		VignetteX("VignetteX",0),
		VignetteY("VignetteY",0),
		Saturation("Saturation",0),
		SaturationMul("SaturationMul",0);
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

	public Vignetting( boolean controlSaturation ) {
		super( ShaderLoader.fromFile( "screenspace", "vignetting", (controlSaturation ? "#define CONTROL_SATURATION" : "") ) );
		rebind();
	}

	public void setIntensity( float intensity ) {
		this.intensity = intensity;
		setParam( Param.VignetteIntensity, intensity );
	}

	public void setSaturation( float saturation ) {
		this.saturation = saturation;
		setParam( Param.Saturation, saturation );
	}

	public void setSaturationMul( float saturationMul ) {
		this.saturationMul = saturationMul;
		setParam( Param.SaturationMul, saturationMul );
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
		setParams( Param.VignetteIntensity, intensity );
		setParams( Param.VignetteX, x );
		setParams( Param.VignetteY, y );
		setParams( Param.Saturation, saturation );
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
