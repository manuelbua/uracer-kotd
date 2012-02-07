package com.bitfire.uracer.effects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.effects.postprocessing.PostProcessEffect;

public class RadialBlur extends PostProcessEffect
{
	private float blur_len = 4.0f; // ctrl quality
	private float blur_width = -0.08f; // ctrl quantity
	private float one_on_blurlen = 1.0f / blur_len;

	private Vector2 origin = new Vector2();

	public RadialBlur()
	{
		ShaderProgram.pedantic = false;
		shader = new ShaderProgram(
					Gdx.files.internal( "data/shaders/radialblur.vert" ).readString(),
					Gdx.files.internal( "data/shaders/radialblur.frag" ).readString()
		);

		if( shader.isCompiled() == false )
			throw new IllegalStateException( "\"" + shader.getLog() + "\"" );

		effectStrength = 0.0f;
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
