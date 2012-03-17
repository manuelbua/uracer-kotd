package com.bitfire.uracer.postprocessing.effects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.postprocessing.IFilter;
import com.bitfire.uracer.postprocessing.IPostProcessorEffect;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.ShaderLoader;

public class ZoomBlur implements IPostProcessorEffect
{
	private int blur_len = 4; // ctrl quality
	private final float MaxBlurWidth = -0.08f; // ctrl quantity

	private Vector2 origin = new Vector2(0,0);
	private ShaderProgram shader;

	public ZoomBlur()
	{
		shader = ShaderLoader.createShader( "zoom-blur", "zoom-blur", "#define BLUR_LENGTH " + blur_len + "\n#define ONE_ON_BLUR_LENGTH " + 1f / (float)blur_len );
		upload();
		setOrigin(0.5f, 0.5f);
		setStrength( 0 );
	}

	private void upload()
	{
		shader.begin();

//		blur_width = -effectStrength;

		shader.setUniformf( "one_on_blurlen", 1f / (float)blur_len );
		shader.setUniformf( "offset_x", origin.x / (float)Gdx.graphics.getWidth() );
		shader.setUniformf( "offset_y", 1f - (origin .y / (float)Gdx.graphics.getHeight()) );
		shader.setUniformi( "u_texture", 0 );

		shader.end();
	}

	public void setOrigin(Vector2 o)
	{
		setOrigin(o.x, o.y);
	}

	public void setStrength(float strength)
	{
		float s = AMath.clamp( strength, 0f, 1f ) * MaxBlurWidth;
		shader.begin();
		shader.setUniformf( "blur_div", s / (float)blur_len );
		shader.end();
	}

	public void setOrigin(float x, float y)
	{
		origin.set(x, y);
		shader.begin();
		shader.setUniformf( "offset_x", origin.x / (float)Gdx.graphics.getWidth() );
		shader.setUniformf( "offset_y", 1f - (origin .y / (float)Gdx.graphics.getHeight()) );
		shader.end();
	}

	@Override
	public void dispose()
	{
		shader.dispose();
	}

	@Override
	public void resume()
	{
		upload();
	}

	@Override
	public void render( FrameBuffer src, FrameBuffer dest )
	{
		if(dest!=null) dest.begin();

		src.getColorBufferTexture().bind( 0 );
		shader.begin();
		IFilter.quad.render( shader );
		shader.end();

		if(dest!=null) dest.end();
	}

}
