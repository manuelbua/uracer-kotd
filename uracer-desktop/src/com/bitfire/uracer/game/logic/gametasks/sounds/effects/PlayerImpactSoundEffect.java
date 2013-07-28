
package com.bitfire.uracer.game.logic.gametasks.sounds.effects;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.events.CarEvent;
import com.bitfire.uracer.game.events.CarEvent.Order;
import com.bitfire.uracer.game.events.CarEvent.Type;
import com.bitfire.uracer.game.logic.gametasks.SoundManager;
import com.bitfire.uracer.game.logic.gametasks.sounds.SoundEffect;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.resources.Sounds;
import com.bitfire.uracer.utils.AMath;

public final class PlayerImpactSoundEffect extends SoundEffect {
	private Sound[] impacts;
	// private long lastSoundTimeMs = 0;
	private float prevFactor = 0;

	// private static final long MinElapsedBetweenSoundsMs = 500;
	// private static final float MinImpactForce = 10f;

	private CarEvent.Listener carEvent = new CarEvent.Listener() {
		@Override
		public void handle (Object source, Type type, Order order) {
			impact(GameEvents.playerCar.data.impulses.len(), ((PlayerCar)source).carState.currSpeedFactor);
		}
	};

	public PlayerImpactSoundEffect () {
		impacts = Sounds.carImpacts;
	}

	@Override
	public void dispose () {
		detach();
		stop();
	}

	private void attach () {
		GameEvents.playerCar.addListener(carEvent, CarEvent.Type.onCollision);
	}

	private void detach () {
		GameEvents.playerCar.removeListener(carEvent, CarEvent.Type.onCollision);
	}

	@Override
	public void player (PlayerCar player) {
		super.player(player);
		if (hasPlayer) {
			attach();
		} else {
			detach();
		}
	}

	private void impact (float impactForce, float speedFactor) {
		float factor = AMath.fixup(AMath.normalizeImpactForce(impactForce));

		// early exit
		if (factor < 0.05f) {
			// FIXME
			// see the bug report at https://code.google.com/p/libgdx/issues/detail?id=1398
			// if (factor > 0) {
			// Gdx.app.log("impact", "Skipping f=" + factor);
			// }
			return;
		}

		// Gdx.app.log("impact", "factor=" + factor + " (prev=" + prevFactor + ")");

		// enough time passed from last impact sound?
		// long millis = System.currentTimeMillis();
		if (/* millis - lastSoundTimeMs >= MinElapsedBetweenSoundsMs || */factor > prevFactor) {
			// lastSoundTimeMs = millis;

			float volumeFactor = 1f;
			int idx = 0;
			prevFactor = factor;

			if (factor > 0.8f) {
				idx = 7;
				volumeFactor = 0.8f + 0.2f * ((factor - 0.8f) / 0.2f);
			} else {
				float range = factor / 0.8f;
				volumeFactor = 0.3f + 0.5f * range;
				idx = (int)(6 * range);
				if (idx > 0 && idx < 6) {
					idx += MathUtils.random(-1, 1);
				}
			}

			play(impacts[idx], volumeFactor * SoundManager.SfxVolumeMul);
			// Gdx.app.log("impact", "playing #" + idx + ", v=" + volumeFactor);
		} else {
			prevFactor = 0;
		}
	}

	@Override
	public void stop () {
		for (int i = 0; i < impacts.length; i++) {
			impacts[i].stop();
		}
	}

	@Override
	public void gameReset () {
		gameRestart();
	}

	@Override
	public void gameRestart () {
		prevFactor = 0;
		// lastSoundTimeMs = 0;
		stop();
	}
}
