package com.bitfire.uracer.postprocessing.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.postprocessing.PostProcessorEffect;
import com.bitfire.uracer.postprocessing.filters.ZoomBlur;

public class Zoom extends PostProcessorEffect
{
	private ZoomBlur zoomBlur;
	private float x, y, strength;

	public Zoom()
	{
		zoomBlur = new ZoomBlur();
	}

	public void setOrigin(Vector2 o)
	{
		this.x = o.x;
		this.y = o.y;
		zoomBlur.setOrigin(o.x, o.y);
	}

	public void setOrigin(float x, float y)
	{
		this.x = x;
		this.y = y;
		zoomBlur.setOrigin( x, y );
	}

	public void setStrength(float strength)
	{
		this.strength = strength;
		zoomBlur.setStrength( strength );
	}

	@Override
	public void dispose()
	{
		zoomBlur.dispose();
	}

	@Override
	public void resume()
	{
		zoomBlur.upload();
		zoomBlur.setOrigin( x, y );
		zoomBlur.setStrength( strength );
	}

	@Override
	public void render( FrameBuffer src, FrameBuffer dest )
	{
		zoomBlur.setInput( src ).setOutput( dest ).render();
	}

}
