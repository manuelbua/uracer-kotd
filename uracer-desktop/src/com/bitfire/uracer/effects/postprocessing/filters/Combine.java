package com.bitfire.uracer.effects.postprocessing.filters;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.bitfire.uracer.utils.ShaderLoader;

public class Combine
{
	private ShaderProgram combine;

	public Combine()
	{
		combine = ShaderLoader.createShader( "bloom/screenspace", "bloom/combine" );
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

	public void render(Texture source1, Texture source2)
	{
		source1.bind(0);
		source2.bind(1);
		combine.begin();
		{
			combine.setUniformi( "u_texture0", 0 );
			combine.setUniformi( "u_texture1", 1 );
			IFilter.quad.render( combine );
		}
		combine.end();
	}
}
