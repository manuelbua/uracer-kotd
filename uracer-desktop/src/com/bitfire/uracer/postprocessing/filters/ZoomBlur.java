package com.bitfire.uracer.postprocessing.filters;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.bitfire.uracer.postprocessing.IFilter;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.ShaderLoader;

public class ZoomBlur extends Filter<ZoomBlur>
{
	private static final int MaxBlurLen = 32;
	private int blur_len; // ctrl quality
	private final float MaxBlurWidth = -0.04f; // ctrl quantity

	private ShaderProgram shader;

	public ZoomBlur()
	{
		this(2);
	}

	public ZoomBlur(int quality)
	{
		this.blur_len = AMath.clamp(quality, 1, MaxBlurLen);
		shader = ShaderLoader.createShader( "zoom-blur", "zoom-blur", "#define BLUR_LENGTH " + blur_len + "\n#define ONE_ON_BLUR_LENGTH " + 1f / (float)blur_len );
		upload();
		setOrigin(0.5f, 0.5f);
		setStrength( 0 );
	}

	public void setOrigin(float x, float y)
	{
		shader.begin();
		shader.setUniformf( "offset_x", x / (float)Gdx.graphics.getWidth() );
		shader.setUniformf( "offset_y", 1f - (y / (float)Gdx.graphics.getHeight()) );
		shader.end();
	}

	public void setStrength(float strength)
	{
		float s = AMath.clamp( strength, 0f, 1f ) * MaxBlurWidth;
		shader.begin();
		shader.setUniformf( "blur_div", s / (float)blur_len );
		shader.end();
	}

	@Override
	public void dispose()
	{
		shader.dispose();
	}

	@Override
	public void upload()
	{
		shader.begin();
		shader.setUniformf( "one_on_blurlen", 1f / (float)blur_len );
		shader.setUniformi( "u_texture", 0 );
		shader.end();
	}

	@Override
	protected void compute()
	{
		inputTexture.bind( 0 );
		shader.begin();
		IFilter.quad.render( shader );
		shader.end();
	}
}
