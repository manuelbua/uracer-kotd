package com.bitfire.uracer.postprocessing.filters;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.bitfire.uracer.postprocessing.IFilter;
import com.bitfire.uracer.utils.ShaderLoader;

public class Combine extends Filter<Combine> {

	public enum Param implements Parameter {
		// @formatter:off
		Texture1("u_texture0",0),
		Texture2("u_texture1",0),
		Source1Intensity("Src1Intensity",0),
		Source1Saturation("Src1Saturation",0),
		Source2Intensity("Src2Intensity",0),
		Source2Saturation("Src2Saturation",0);
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

	private Texture inputTexture2 = null;

	public Combine() {
		super( ShaderLoader.fromFile( "screenspace", "combine" ) );
		rebind();
	}

	public Combine setInput( FrameBuffer buffer1, FrameBuffer buffer2 ) {
		this.inputTexture = buffer1.getColorBufferTexture();
		this.inputTexture2 = buffer2.getColorBufferTexture();
		return this;
	}

	public Combine setInput( Texture texture1, Texture texture2 ) {
		this.inputTexture = texture1;
		this.inputTexture2 = texture2;
		return this;
	}

	@Override
	public final void rebind() {
		setParams( Param.Texture1, u_texture_1 );
		setParams( Param.Texture2, u_texture_2 );
		endParams();
	}

	@Override
	protected void compute() {
		inputTexture.bind( u_texture_1 );
		inputTexture2.bind( u_texture_2 );
		program.begin();
		IFilter.quad.render( program );
		program.end();
	}
}
