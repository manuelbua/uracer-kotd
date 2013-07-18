
package com.bitfire.uracer.game.logic.gametasks.sounds.effects;

import com.badlogic.gdx.audio.Sound;
import com.bitfire.uracer.game.logic.gametasks.SoundManager;
import com.bitfire.uracer.game.logic.gametasks.sounds.SoundEffect;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.resources.Sounds;
import com.bitfire.uracer.utils.AMath;

public final class PlayerEngineSoundEffect extends SoundEffect {
	private Sound carEngine = null;
	private long carEngineId = -1;
	private float carEnginePitchStart = 0;
	private float carEnginePitchLast = 0;
	private static final float CarEnginePitchMin = 0.8f;
	private boolean started;

	public PlayerEngineSoundEffect () {
		carEngine = Sounds.carEngine;
	}

	@Override
	public void dispose () {
		stop();
	}

	@Override
	public void tick () {
		if (!isPaused && hasPlayer && carEngineId > -1) {
			float speedFactor = player.carState.currSpeedFactor;

			float pitch = CarEnginePitchMin + speedFactor * 1.25f;

			// float pitch = carEnginePitchMin + 0.8f;
			if (!AMath.equals(pitch, carEnginePitchLast)) {
				carEngine.setPitch(carEngineId, pitch);
				carEnginePitchLast = pitch;
			}
		}
	}

	private void start () {
		if (started || !hasPlayer) {
			return;
		}

		started = true;
		carEngineId = loop(carEngine, SoundManager.SfxVolumeMul);
		carEngine.setPitch(carEngineId, carEnginePitchStart);
	}

	@Override
	public void stop () {
		if (!started) {
			return;
		}

		carEngine.stop();
		started = false;
	}

	@Override
	public void gameReset () {
		stop();
		carEnginePitchStart = CarEnginePitchMin;
		carEnginePitchLast = CarEnginePitchMin;
	}

	@Override
	public void player (PlayerCar player) {
		super.player(player);

		if (hasPlayer) {
			start();
		} else {
			stop();
		}
	}
}
