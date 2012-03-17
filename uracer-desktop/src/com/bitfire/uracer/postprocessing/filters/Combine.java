package com.bitfire.uracer.postprocessing.filters;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.bitfire.uracer.postprocessing.IFilter;
import com.bitfire.uracer.utils.ShaderLoader;

public class Combine extends Filter<Combine>
{
	private ShaderProgram combine;
	private Texture inputTexture2 = null;

	public Combine()
	{
		combine = ShaderLoader.createShader( "screenspace", "combine" );
		upload();
	}

	public void dispose()
	{
		combine.dispose();
	}

	public enum Param
	{
		Source1Intensity("Src1Intensity"),
		Source1Saturation("Src1Saturation"),
		Source2Intensity("Src2Intensity"),
		Source2Saturation("Src2Saturation");

		final String mnemonic;
		private Param(String m)
		{
			this.mnemonic = m;
		}
	}

	public void setParam(Param param, float value)
	{
		combine.begin();
		combine.setUniformf( param.mnemonic, value );
		combine.end();
	}

	public Combine setInput(FrameBuffer buffer1, FrameBuffer buffer2)
	{
		this.inputTexture = buffer1.getColorBufferTexture();
		this.inputTexture2 = buffer2.getColorBufferTexture();
		return this;
	}

	public Combine setInput(Texture texture1, Texture texture2)
	{
		this.inputTexture = texture1;
		this.inputTexture2 = texture2;
		return this;
	}

	@Override
	public void upload()
	{
		combine.begin();
		combine.setUniformi( "u_texture0", u_texture_1 );
		combine.setUniformi( "u_texture1", u_texture_2 );
		combine.end();
	}

	@Override
	protected void compute()
	{
		inputTexture.bind(u_texture_1);
		inputTexture2.bind(u_texture_2);
		combine.begin();
		IFilter.quad.render( combine );
		combine.end();
	}
}
