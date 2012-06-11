package com.bitfire.uracer.postprocessing.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.postprocessing.PostProcessorEffect;
import com.bitfire.uracer.postprocessing.filters.RadialBlur;
import com.bitfire.uracer.postprocessing.filters.Zoom;

/** Implements a zooming effect: either a radial blur filter or a zoom filter is used. */
public class Zoomer extends PostProcessorEffect {
	private boolean doRadial = false;
	private RadialBlur radialBlur = null;
	private Zoom zoom = null;

	public Zoomer( RadialBlur.Quality quality ) {
		radialBlur = new RadialBlur( quality );
		zoom = null;

		doRadial = true;
	}

	public Zoomer() {
		radialBlur = null;
		zoom = new Zoom();

		doRadial = false;
	}

	public void setOrigin( Vector2 o ) {
		if( doRadial ) {
			radialBlur.setOrigin( o.x, o.y );
		} else {
			zoom.setOrigin( o.x, o.y );
		}
	}

	public void setOrigin( float x, float y ) {
		if( doRadial ) {
			radialBlur.setOrigin( x, y );
		} else {
			zoom.setOrigin( x, y );
		}
	}

	public void setBlurStrength( float strength ) {
		if( doRadial ) {
			radialBlur.setStrength( strength );

			if( strength == 0 && isEnabled() ) {
				setEnabled( false );
			} else if( strength != 0 && !isEnabled() ) {
				setEnabled( true );
			}
		}
	}

	public void setZoom( float zoom ) {
		if( doRadial ) {
			radialBlur.setZoom( zoom );
		} else {
			this.zoom.setZoom( zoom );
		}
	}

	@Override
	public void dispose() {
		if( radialBlur != null ) {
			radialBlur.dispose();
			radialBlur = null;
		}

		if( zoom != null ) {
			zoom.dispose();
			zoom = null;
		}
	}

	@Override
	public void rebind() {
		radialBlur.rebind();
	}

	@Override
	public void render( FrameBuffer src, FrameBuffer dest ) {
		if( doRadial ) {
			radialBlur.setInput( src ).setOutput( dest ).render();
		} else {
			zoom.setInput( src ).setOutput( dest ).render();
		}
	}
}