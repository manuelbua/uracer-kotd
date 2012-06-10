package com.bitfire.uracer.postprocessing.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.postprocessing.PostProcessorEffect;
import com.bitfire.uracer.postprocessing.filters.RadialBlur;

/** Implements a zooming effect with a radial blur to mimic a sense of motion. */
public class Zoom extends PostProcessorEffect {
	private RadialBlur radialBlur;

	public Zoom( RadialBlur.Quality quality ) {
		radialBlur = new RadialBlur( quality );
	}

	public void setOrigin( Vector2 o ) {
		radialBlur.setOrigin( o.x, o.y );
	}

	public void setOrigin( float x, float y ) {
		radialBlur.setOrigin( x, y );
	}

	public void setStrength( float strength ) {
		radialBlur.setStrength( strength );

		if( strength == 0 && isEnabled() ) {
			setEnabled( false );
		} else if( strength != 0 && !isEnabled() ) {
			setEnabled( true );
		}
	}

	public void setZoom( float zoom ) {
		radialBlur.setZoom( zoom );
	}

	@Override
	public void dispose() {
		radialBlur.dispose();
	}

	@Override
	public void rebind() {
		radialBlur.rebind();
	}

	@Override
	public void render( FrameBuffer src, FrameBuffer dest ) {
		radialBlur.setInput( src ).setOutput( dest ).render();
	}
}