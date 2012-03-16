package com.bitfire.uracer.postprocessing;
/*
package com.bitfire.uracer.effects.postprocessing;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.experimental.ShaderLoader;
import com.badlogic.gdx.math.Vector2;

public class RadialBlur extends PostProcessEffect
{
	private float blur_len = 4.0f; // ctrl quality
	private float blur_width = -0.08f; // ctrl quantity
	private float one_on_blurlen = 1.0f / blur_len;

	private Vector2 origin = new Vector2();

	public RadialBlur()
	{
		setShader( ShaderLoader.createShader( "data/shaders/radialblur/radialblur.vert", "data/shaders/radialblur/radialblur.vert" ) );
		effectStrength = 0f;
	}

	@Override
	protected void onBeforeShaderPass()
	{
		blur_width = -effectStrength;

		float x = origin.x / (float)Gdx.graphics.getWidth();
		float y = 1 - origin .y / (float)Gdx.graphics.getHeight();

		shader.setUniformf( "offset_x", x );
		shader.setUniformf( "offset_y", y );

		float blur_div = blur_width / blur_len;
		shader.setUniformf( "blur_div", blur_div );
		shader.setUniformf( "one_on_blurlen", one_on_blurlen );
	}

	public void setOrigin(Vector2 o)
	{
		origin.set(o);
	}

	public void setOrigin(float x, float y)
	{
		origin.set(x, y);
	}
}
*/