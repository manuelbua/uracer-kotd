
package com.bitfire.uracer.game.logic.gametasks.trackeffects;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.game.logic.gametasks.PlayerClient;

public abstract class TrackEffect extends PlayerClient implements Disposable {
	protected boolean isPaused = false;
	public final TrackEffectType type;

	public TrackEffect (TrackEffectType what) {
		this.type = what;
	}

	public abstract void tick ();

	public abstract void reset ();

	/* The used GameRenderer instance is being passed for utilities, such as querying visibility */
	public abstract void render (SpriteBatch batch);

	public abstract int getParticleCount ();

	public abstract int getMaxParticleCount ();

	public void gamePause () {
		isPaused = true;
	}

	public void gameResume () {
		isPaused = false;
	}

	public boolean isPaused () {
		return isPaused;
	}
}
