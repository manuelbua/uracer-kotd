package com.bitfire.uracer.effects.postprocessing.filters;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.bitfire.uracer.utils.ShaderLoader;

public class Threshold extends Filter
{
	private ShaderProgram threshold;

	private float gamma = 0;

	public Threshold(boolean useAlphaChannelAsMask)
	{
		threshold = null;

		if(useAlphaChannelAsMask)
			threshold = ShaderLoader.createShader( "bloom/screenspace", "bloom/masked-threshold" );
		else
			threshold = ShaderLoader.createShader( "bloom/screenspace", "bloom/threshold" );

	}

	public void dispose()
	{
		threshold.dispose();
	}

	public void setTreshold(float gamma)
	{
		this.gamma = gamma;
		upload();
	}

	public void upload()
	{
		threshold.begin();
		{
			threshold.setUniformf( "treshold", gamma );
			threshold.setUniformf( "tresholdInvTx", (1f / (1f-gamma)) );	// correct
//			shThreshold.setUniformf( "tresholdInvTx", (1f / (gamma)) );		// does this look better?
		}

		threshold.end();
	}

	@Override
	public void render(Texture source)
	{
		source.bind(0);

		threshold.begin();
		{
			threshold.setUniformi( "u_texture0", 0 );
			IFilter.quad.render( threshold );
		}
		threshold.end();
	}
}
