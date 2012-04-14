package com.bitfire.uracer.game.audio;

import com.badlogic.gdx.audio.Sound;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Sounds;
import com.bitfire.uracer.game.data.GameData;
import com.bitfire.uracer.utils.AMath;

public final class CarEngineSoundEffect extends CarSoundEffect {
	private Sound carEngine = null;
	private long carEngineId = -1;
	private static float carEnginePitchStart = 0;
	private float carEnginePitchLast = 0;
	private static final float carEnginePitchMin = 1f;

	public CarEngineSoundEffect() {
		 carEngine = Sounds.carEngine;
	}

	@Override
	public void onDispose() {
	}

	@Override
	public void onTick() {
		if( carEngineId > -1 ) {
			float speedFactor = GameData.States.playerState.currSpeedFactor;

			float pitch = carEnginePitchMin + speedFactor * 0.65f;
			if( !AMath.equals( pitch, carEnginePitchLast ) ) {
				carEngine.setPitch( carEngineId, pitch );
				carEnginePitchLast = pitch;
			}
		}
	}

	@Override
	public void onStart() {
		if( Config.isDesktop ) {
			carEngineId = carEngine.loop( 1f );
		} else {
			// UGLY HACK FOR ANDROID
			carEngineId = checkedLoop( carEngine, 1f );
		}

		onReset();
	}

	@Override
	public void onStop() {
		carEngine.stop();
	}

	@Override
	public void onReset() {
		onStop();
		carEnginePitchStart = carEnginePitchMin;
		carEnginePitchLast = carEnginePitchMin;
		carEngine.setPitch( carEngineId, carEnginePitchStart );
	}
}
