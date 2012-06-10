package com.bitfire.uracer.postprocessing.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.postprocessing.PostProcessorEffect;
import com.bitfire.uracer.postprocessing.filters.RadialBlur;

/** Implements a zooming effect with a radial blur to mimic a sense of motion. */
public class Zoom extends PostProcessorEffect {
	private RadialBlur zoomBlur;

	public Zoom( RadialBlur.Quality quality ) {
		zoomBlur = new RadialBlur( quality );
	}

	public void setOrigin( Vector2 o ) {
		zoomBlur.setOrigin( o.x, o.y );
	}

	public void setOrigin( float x, float y ) {
		zoomBlur.setOrigin( x, y );
	}

	public void setStrength( float strength ) {
		zoomBlur.setStrength( strength );

		if( strength == 0 && isEnabled() ) {
			setEnabled( false );
		} else if( strength != 0 && !isEnabled() ) {
			setEnabled( true );
		}
	}

	@Override
	public void dispose() {
		zoomBlur.dispose();
	}

	@Override
	public void rebind() {
		zoomBlur.rebind();
	}

	@Override
	public void render( FrameBuffer src, FrameBuffer dest ) {
		zoomBlur.setInput( src ).setOutput( dest ).render();
	}
}