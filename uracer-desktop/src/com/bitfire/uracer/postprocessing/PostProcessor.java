package com.bitfire.uracer.postprocessing;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;

public class PostProcessor
{
	private static FrameBuffer frameBuffer;
	private static SpriteBatch quad;
	private static PostProcessEffect effect;

	public static void init( int rttWidth, int rttHeight )
	{
		frameBuffer = new FrameBuffer( Format.RGB565, rttWidth, rttHeight, true );
		frameBuffer.getColorBufferTexture().setFilter( TextureFilter.Nearest, TextureFilter.Nearest );
		frameBuffer.getColorBufferTexture().setWrap( TextureWrap.ClampToEdge, TextureWrap.ClampToEdge );
		quad = new SpriteBatch();
		effect = null;
	}

	public static void begin()
	{
		if( hasEffect() )
		{
			frameBuffer.begin();
		}
	}

	public static void end()
	{
		if( hasEffect() )
		{
			frameBuffer.end();
			render();
		}
	}

	public static boolean hasEffect()
	{
		return (effect != null) && effect.isEnabled();
	}

	public static void setEffect( PostProcessEffect effect )
	{
		PostProcessor.effect = effect;
	}

	private static void render()
	{
		quad.setShader( effect.getShader() );
		quad.begin();
		effect.onBeforeShaderPass();
		quad.draw( frameBuffer.getColorBufferTexture(), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0, 0,
				frameBuffer.getWidth(), frameBuffer.getHeight(), false, true );
		quad.end();
	}
}
