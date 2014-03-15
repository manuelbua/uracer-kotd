
package com.bitfire.uracer.game.logic.gametasks.sounds.effects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.TimeUtils;
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
	private static final float MinImpactForce = 0.05f;
	private static final long MinMillisBeforePlay = 500;
	private Sound[] impacts;
	private long lastMillis = 0;
	private float lastFactor = 0;
	private int lastIdx = 0;

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
		if (factor < MinImpactForce) {
			// if (factor > 0) {
			// Gdx.app.log("impact", "Skipping f=" + factor);
			// }
			return;
		}

		// Gdx.app.log("impact", "factor=" + factor + " (prev=" + prevFactor + ")");

		long millis = TimeUtils.millis();
		if (millis - lastMillis > MinMillisBeforePlay || factor > lastFactor) {
			lastMillis = millis;
			lastFactor = factor;

			float vol = 0;
			float volumeFactor = 1f;
			int idx = 0;

			if (factor > 0.9f) {
				// high (1)
				idx = 7;
				volumeFactor = factor;
			} else {
				float range = factor / 0.9f;
				if (range < 0.5) {
					// low (4)
					do {
						idx = MathUtils.random(0, 3);
					} while (idx == lastIdx);
				} else {
					// mid (3)
					do {
						idx = MathUtils.random(4, 6);
					} while (idx == lastIdx);
				}

				volumeFactor = 0.2f + 0.7f * range;
			}

			lastIdx = idx;
			vol = volumeFactor * 1 * SoundManager.SfxVolumeMul;
			long result = play(impacts[idx], vol);

			if (result == -1) {
				Gdx.app.log("PlayerImpactSoundEffect", "Couldn't play impact sound sample #" + idx + "(v=" + vol
					+ "), no free sources available for concurrent playing");
			} else {
				Gdx.app.log("PlayerImpactSoundEffect", "playing #" + idx + ", v=" + vol);
			}
		} else {
			if (factor < lastFactor) {
				lastFactor = 0;
			}
		}
	}

	@Override
	public void stop () {
		for (int i = 0; i < impacts.length; i++) {
			impacts[i].stop();
		}
	}

	@Override
	public void gameRestart () {
		lastMillis = 0;
		lastFactor = 0;
		lastIdx = 0;
		stop();
	}

	@Override
	public void gameReset () {
		gameRestart();
	}
}
