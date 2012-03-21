package com.bitfire.uracer.postprocessing.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.postprocessing.PostProcessorEffect;
import com.bitfire.uracer.postprocessing.filters.ZoomBlur;

public class Zoom extends PostProcessorEffect {
	private ZoomBlur zoomBlur;
	private float x, y, strength;

	public Zoom( int quality ) {
		zoomBlur = new ZoomBlur( quality );
	}

	public void setOrigin( Vector2 o ) {
		this.x = o.x;
		this.y = o.y;
		zoomBlur.setOrigin( o.x, o.y );
	}

	public void setOrigin( float x, float y ) {
		this.x = x;
		this.y = y;
		zoomBlur.setOrigin( x, y );
	}

	public void setMaxStrength( float maxStrength ) {
		zoomBlur.setMaxStrength( maxStrength );
	}

	public void setStrength( float strength ) {
		this.strength = strength;
		zoomBlur.setStrength( strength );
	}

	public void dispose() {
		zoomBlur.dispose();
	}

	public void rebind() {
		zoomBlur.upload();
		zoomBlur.setOrigin( x, y );
		zoomBlur.setStrength( strength );
	}

	public void render( FrameBuffer src, FrameBuffer dest ) {
		zoomBlur.setInput( src ).setOutput( dest ).render();
	}
}