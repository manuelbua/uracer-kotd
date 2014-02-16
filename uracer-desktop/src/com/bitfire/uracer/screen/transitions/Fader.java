
package com.bitfire.uracer.screen.transitions;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.bitfire.postprocessing.utils.FullscreenQuad;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.screens.GameScreensFactory.ScreenType;
import com.bitfire.uracer.screen.Screen;
import com.bitfire.uracer.screen.ScreenFactory;
import com.bitfire.uracer.screen.ScreenFactory.ScreenId;
import com.bitfire.uracer.screen.ScreenTransition;
import com.bitfire.uracer.screen.ScreenUtils;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.utils.ShaderLoader;

/** Implements a fader, transitioning from one screen to another by first transitioning the current screen to the specified color,
 * then transitioning from the specified color to the next screen. */
public final class Fader extends ScreenTransition {
	static final long MaxFrameStep = (long)(Config.Physics.Dt * 1000000000f);
	long duration, elapsed, half; // in nanoseconds
	float factor;
	FrameBuffer from, to;
	FullscreenQuad quad;
	ShaderProgram fade;
	Screen next;
	ScreenId nextType;
	Color color = Color.BLACK;
	boolean nextPrepared, delayNextHalf;

	public Fader (ScreenFactory factory) {
		super(factory);
		quad = new FullscreenQuad();
		fade = ShaderLoader.fromFile("fade", "fade");
		setDuration(1000);
		reset();
	}

	public void setColor (Color color) {
		this.color.set(color);
	}

	private void rebind () {
		fade.begin();
		fade.setUniformi("u_texture0", 0);
		fade.setUniformi("u_texture1", 1);
		fade.setUniformf("Ratio", 0);
		fade.end();
	}

	@Override
	public void reset () {
		rebind();
		next = null;
		factor = 0;
		elapsed = 0;
		setDuration(duration / 1000000);
		nextPrepared = false;
		nextType = ScreenType.NoScreen;
	}

	@Override
	public void dispose () {
		quad.dispose();
		fade.dispose();
	}

	@Override
	public void frameBuffersReady (Screen current, FrameBuffer from, ScreenId nextScreen, FrameBuffer to) {
		this.from = from;
		this.to = to;
		this.nextType = nextScreen;

		ScreenUtils.copyScreen(current, from);
		ScreenUtils.clear(to, Color.BLACK);
	}

	@Override
	public Screen nextScreen () {
		return next;
	}

	/** Sets the duration of the effect, in milliseconds. */
	@Override
	public void setDuration (long durationMs) {
		if (durationMs == 0) {
			throw new GdxRuntimeException("Invalid transition duration specified.");
		}

		duration = durationMs * 1000000;
		half = duration / 2;
	}

	@Override
	public void resume () {
		rebind();
	}

	@Override
	public void update () {
		long delta = (long)URacer.Game.getLastDeltaNs();
		delta = AMath.clamp(delta, 0, MaxFrameStep);
		// Gdx.app.log("Fader", "delta=" + URacer.Game.getLastDeltaNs() + ", e=" + elapsed + ", d=" + duration + ", f=" + factor);

		elapsed += delta;

		if (elapsed > duration) {
			elapsed = duration;
		}

		if (elapsed < half) {
			factor = 1 - (float)(half - elapsed) / (float)half;
			delayNextHalf = true;
		} else {
			if (delayNextHalf) {
				// ensures the other half will start at perfect time
				factor = 1f;
				delayNextHalf = false;
			} else {
				factor = (float)(elapsed - half) / (float)half;

				if (!nextPrepared) {
					nextPrepared = true;
					next = createScreen(nextType);
					ScreenUtils.clear(from, Color.BLACK);
					ScreenUtils.copyScreen(next, to);
				}
			}
		}
	}

	@Override
	public void render () {
		from.getColorBufferTexture().bind(0);
		to.getColorBufferTexture().bind(1);

		fade.begin();
		fade.setUniformf("Ratio", factor);
		quad.render(fade);
		fade.end();
	}

	@Override
	public boolean isComplete () {
		return elapsed >= duration;
	}
}
