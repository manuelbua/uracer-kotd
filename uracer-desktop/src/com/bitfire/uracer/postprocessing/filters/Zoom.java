package com.bitfire.uracer.postprocessing.filters;

import com.badlogic.gdx.Gdx;
import com.bitfire.uracer.utils.ShaderLoader;

public final class Zoom extends Filter<Zoom> {
	private float x, y, zoom;

	public enum Param implements Parameter {
		// @formatter:off
		Texture( "u_texture0", 0 ),
		OffsetX( "offset_x", 0 ),
		OffsetY( "offset_y", 0 ),
		Zoom( "zoom", 0 ),
		;
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

	public Zoom() {
//		super(null);
//
//		String vertexShaderSrc = Gdx.files.internal( "data/shaders/zoom.vertex" ).readString();
//		String fragmentShaderSrc = Gdx.files.external( Config.URacerConfigFolder + "/zoom.fragment").readString();
//
//		ShaderProgram.pedantic = false;
//		ShaderProgram shader = new ShaderProgram( vertexShaderSrc, fragmentShaderSrc );
//		if( !shader.isCompiled() ) {
//			Gdx.app.log( "Zoom::ShaderLoader", shader.getLog() );
//			Gdx.app.exit();
//		} else {
//			Gdx.app.log( "Zoom::ShaderLoader", shader.getLog() );
//		}
//
//		this.program = shader;

		super( ShaderLoader.fromFile( "zoom", "zoom" ) );

		rebind();
		setOrigin( 0.5f, 0.5f );
		setZoom( 1f );
	}

	public void setOrigin( float x, float y ) {
		this.x = x / (float)Gdx.graphics.getWidth();
		this.y = 1f - (y / (float)Gdx.graphics.getHeight());
		setParams( Param.OffsetX, this.x );
		setParams( Param.OffsetY, this.y );
		endParams();
	}

	public void setZoom( float zoom ) {
		this.zoom = 1f / zoom;
		setParam( Param.Zoom, this.zoom );
	}

	@Override
	public void rebind() {
		// reimplement super for batching every parameter
		setParams( Param.Texture, u_texture0 );
		setParams( Param.OffsetX, x );
		setParams( Param.OffsetY, y );
		setParams( Param.Zoom, zoom );
		endParams();
	}


}
