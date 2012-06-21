package com.bitfire.uracer.screen.transitions;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.postprocessing.FullscreenQuad;
import com.bitfire.uracer.utils.ShaderLoader;

public class Fader extends ScreenTransition {
	FrameBuffer from, to;
	float durationSecs;
	float elapsedSecs;
	float factor;
	FullscreenQuad quad;
	ShaderProgram fade;

	public Fader( float durationSecs ) {
		this.durationSecs = durationSecs;
		quad = new FullscreenQuad();
		fade = ShaderLoader.fromFile( "fade", "fade" );
		rebind();
	}

	private void rebind() {
		fade.begin();
		fade.setUniformi( "u_texture0", 0 );
		fade.setUniformi( "u_texture1", 1 );
		fade.end();
	}

	@Override
	public void init( FrameBuffer curr, FrameBuffer next ) {
		from = curr;
		to = next;
		elapsedSecs = 0;
		factor = 0;
	}

	@Override
	public void dispose() {
		quad.dispose();
		fade.dispose();
	}

	@Override
	public void update() {
		elapsedSecs += URacer.getLastDeltaSecs();

		if( elapsedSecs < 0 ) {
			return;
		}

		if( elapsedSecs > durationSecs ) {
			elapsedSecs = durationSecs;
		}

		factor = elapsedSecs / durationSecs;
	}

	@Override
	public void render() {
		from.getColorBufferTexture().bind(0);
		to.getColorBufferTexture().bind(1);

		fade.begin();
		fade.setUniformf( "Ratio", factor );
		quad.render( fade );
		fade.end();
	}

	@Override
	public boolean hasFinished() {
		return elapsedSecs >= durationSecs;
	}
}
