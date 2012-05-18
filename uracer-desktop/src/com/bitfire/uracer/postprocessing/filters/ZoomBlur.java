package com.bitfire.uracer.postprocessing.filters;

import com.badlogic.gdx.Gdx;
import com.bitfire.uracer.postprocessing.IFilter;
import com.bitfire.uracer.utils.ShaderLoader;

public final class ZoomBlur extends Filter<ZoomBlur> {
	// ctrl quality
	private int blur_len;

	// ctrl quantity
	private float strength, x, y;

	public ZoomBlur() {
		this( Quality.Low );
	}

	public enum Quality {
		// @formatter:off
		VeryHigh(16),
		High( 8 ),
		Normal( 5 ),
		Medium( 4 ),
		Low( 2 );
		// @formatter:off

		final int length;

		private Quality( int value ) {
			this.length = value;
		}
	}

	public enum Param implements Parameter {
		// @formatter:off
		Texture( "u_texture", 0 ),
		BlurDiv( "blur_div", 0 ),
		OffsetX( "offset_x", 0 ),
		OffsetY( "offset_y", 0 ),
		OneOnBlurLen( "one_on_blurlen", 0 );
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

	public ZoomBlur( Quality quality ) {
		super( ShaderLoader.fromFile( "zoom-blur", "zoom-blur", "#define BLUR_LENGTH " + quality.length + "\n#define ONE_ON_BLUR_LENGTH " + 1f / (float)quality.length ) );
		this.blur_len = quality.length;
		rebind();
		setOrigin( 0.5f, 0.5f );
		setStrength( 0.5f );
	}

	public void setOrigin( float x, float y ) {
		this.x = x;
		this.y = y;
		setParams( Param.OffsetX, x / (float)Gdx.graphics.getWidth() );
		setParams( Param.OffsetY, 1f - (y / (float)Gdx.graphics.getHeight()) ).endParams();
	}

	public void setStrength( float strength ) {
		this.strength = strength;
		setParams( Param.BlurDiv, strength / (float)blur_len ).endParams();
	}

	@Override
	public void rebind() {
		setParams( Param.Texture, u_texture_0 );
		setParams( Param.OneOnBlurLen, 1f / (float)blur_len );
		setParams( Param.BlurDiv, this.strength / (float)blur_len );

		// being explicit (could call setOrigin that will call endParams)
		setParams( Param.OffsetX, x / (float)Gdx.graphics.getWidth() );
		setParams( Param.OffsetY, 1f - (y / (float)Gdx.graphics.getHeight()) ).endParams();
	}

	@Override
	protected void compute() {
		inputTexture.bind( u_texture_0 );
		program.begin();
		IFilter.quad.render( program );
		program.end();
	}
}
