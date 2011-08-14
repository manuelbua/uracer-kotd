package com.bitfire.uracer.postprocessing.effects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Physics;
import com.bitfire.uracer.postprocessing.PostProcessEffect;
import com.bitfire.uracer.utils.AMath;

public class RadialBlur extends PostProcessEffect
{
	private float blur_len = 4.0f; // ctrl quality
	private float blur_width = -0.08f; // ctrl quantity
	private float one_on_blurlen = 1.0f / blur_len;

	private float effectStrength = 0.0f;
	private final float MaxStrength = 0.25f;
	private Vector2 origin = new Vector2();

	public RadialBlur()
	{
		shader = new ShaderProgram( Gdx.files.internal( "data/shaders/radialblur.vert" ).readString(), Gdx.files.internal(
				"data/shaders/radialblur.frag" ).readString() );

		if( shader.isCompiled() == false )
			throw new IllegalStateException( "\"" + shader.getLog() + "\"" );

		effectStrength = 0.0f;
		enabled = false;
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

	public float strength()
	{
		return effectStrength;
	}

	public void setOrigin(Vector2 o)
	{
		origin.set(o);
	}

	public void setOrigin(float x, float y)
	{
		origin.set(x, y);
	}

	public void setStrength( float aStrength )
	{
		effectStrength = AMath.clamp( aStrength, 0, MaxStrength );
	}

	public void addStrength( float aStrength )
	{
		setStrength( effectStrength + aStrength );
	}

	public void dampStrength( float factor )
	{
		setStrength( effectStrength * (float)Math.pow( (1.0f-factor), Physics.dt ) );
	}
}
