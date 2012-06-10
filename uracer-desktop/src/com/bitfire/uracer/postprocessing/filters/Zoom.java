package com.bitfire.uracer.postprocessing.filters;

import com.badlogic.gdx.Gdx;
import com.bitfire.uracer.utils.ShaderLoader;

public final class Zoom extends Filter<Zoom> {
	private float x, y, zoom;

	public enum Param implements Parameter {
		// @formatter:off
		Texture( "u_texture", 0 ),
		OffsetX( "offset_x", 0 ),
		OffsetY( "offset_y", 0 ),
		Zoom("zoom",0),
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
		super( ShaderLoader.fromFile( "zoom", "zoom" ) );
		rebind();
		setOrigin( 0.5f, 0.5f );
		setZoom( 1f );
	}

	public void setOrigin( float x, float y ) {
		this.x = x;
		this.y = y;
		setParams( Param.OffsetX, x / (float)Gdx.graphics.getWidth() );
		setParams( Param.OffsetY, 1f - (y / (float)Gdx.graphics.getHeight()) );
		endParams();
	}

	public void setZoom( float zoom ) {
		this.zoom = 1f / zoom;
		setParam( Param.Zoom, this.zoom );
	}

	@Override
	public void rebind() {
		setParams( Param.Texture, u_texture0 );

		setParams( Param.OffsetX, x / (float)Gdx.graphics.getWidth() );
		setParams( Param.OffsetY, 1f - (y / (float)Gdx.graphics.getHeight()) );

		setParams( Param.Zoom, zoom );

		endParams();
	}
}
