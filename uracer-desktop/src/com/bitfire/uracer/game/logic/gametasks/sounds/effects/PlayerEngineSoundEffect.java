
package com.bitfire.uracer.game.logic.gametasks.sounds.effects;

import com.badlogic.gdx.audio.Sound;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.game.logic.gametasks.sounds.SoundEffect;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.resources.Sounds;
import com.bitfire.uracer.utils.AMath;

public final class PlayerEngineSoundEffect extends SoundEffect {
	private Sound carEngine = null;
	private long carEngineId = -1;
	private float carEnginePitchStart = 0;
	private float carEnginePitchLast = 0;
	private static final float carEnginePitchMin = 0.8f;
	private boolean started;

	public PlayerEngineSoundEffect () {
		carEngine = Sounds.carEngine;
	}

	@Override
	public void dispose () {
	}

	@Override
	public void tick () {
		if (carEngineId > -1) {
			float speedFactor = player.carState.currSpeedFactor;

			float pitch = carEnginePitchMin + speedFactor * 1.25f;
			// float pitch = carEnginePitchMin + 0.8f;
			if (!AMath.equals(pitch, carEnginePitchLast)) {
				carEngine.setPitch(carEngineId, pitch);
				carEnginePitchLast = pitch;
			}
		}
	}

	@Override
	public void start () {
		if (started) {
			return;
		}

		started = true;

		if (URacer.Game.isDesktop()) {
			carEngineId = carEngine.loop(1f);
		} else {
			// UGLY HACK FOR ANDROID
			carEngineId = checkedLoop(carEngine, 1f);
		}
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
		carEnginePitchStart = carEnginePitchMin;
		carEnginePitchLast = carEnginePitchMin;
		carEngine.setPitch(carEngineId, carEnginePitchStart);
		started = false;
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
