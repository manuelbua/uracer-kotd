package com.bitfire.uracer.postprocessing.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.bitfire.uracer.postprocessing.PostProcessorEffect;
import com.bitfire.uracer.postprocessing.filters.Vignetting;

public class Vignette extends PostProcessorEffect {
	private Vignetting vignetting;

	public Vignette() {
		vignetting = new Vignetting();
	}

	@Override
	public void dispose() {
		vignetting.dispose();
	}

//	public void setIntensity( float intensity ) {
//		vignetting.setIntensity( intensity );
//	}

	public void setCoords(float x, float y) {
		vignetting.setCoords( x, y );
	}

	public void setX(float x) {
		vignetting.setX( x );
	}

	public void setY(float y) {
		vignetting.setY( y );
	}

	@Override
	public void rebind() {
		vignetting.rebind();
	}

	@Override
	public void render( FrameBuffer src, FrameBuffer dest ) {
		vignetting.setInput( src ).setOutput( dest ).render();
	};
}
