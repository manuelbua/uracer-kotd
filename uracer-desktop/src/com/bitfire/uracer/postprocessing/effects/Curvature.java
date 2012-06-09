package com.bitfire.uracer.postprocessing.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.bitfire.uracer.postprocessing.PostProcessorEffect;
import com.bitfire.uracer.postprocessing.filters.RadialDistortion;

public class Curvature extends PostProcessorEffect {
	private RadialDistortion distort;

	public Curvature() {
		distort = new RadialDistortion();
	}

	@Override
	public void dispose() {
		distort.dispose();
	}

	public void setDistortion( float distortion ) {
		distort.setDistortion( distortion );
	}

	public void setZoom( float zoom ) {
		distort.setZoom( zoom );
	}

	@Override
	public void rebind() {
		distort.rebind();
	}

	@Override
	public void render( FrameBuffer src, FrameBuffer dest ) {
		distort.setInput( src ).setOutput( dest ).render();
	};

}
