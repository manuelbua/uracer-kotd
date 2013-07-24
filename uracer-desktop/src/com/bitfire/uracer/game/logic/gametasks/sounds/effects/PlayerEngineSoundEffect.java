
package com.bitfire.uracer.game.logic.gametasks.sounds.effects;

import com.badlogic.gdx.audio.Sound;
import com.bitfire.uracer.game.logic.gametasks.sounds.SoundEffect;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.resources.Sounds;

public final class PlayerEngineSoundEffect extends SoundEffect {
	private int NumTracks = 7;
	private Sound[] engine = new Sound[NumTracks];
	private long[] mid = new long[NumTracks];
	private boolean[] started = new boolean[NumTracks];

	public PlayerEngineSoundEffect () {
		engine = Sounds.carEngine;
	}

	@Override
	public void dispose () {
		stop();
	}

	@Override
	public void tick () {
		float speedFactor = player.carState.currSpeedFactor;

		// idle
		setVolume(0, 0.3f);
		setPitch(0, 1f); // 1 -> 1.85f (switch to #1)

		// on, low
		setVolume(1, 0.3f);
		setPitch(1, 1f); // 1 -> 1.5f (switch to #2)

		// // idle
		// setVolume(0, 0.3f);
		// setPitch(0, 1f); // 1 -> 1.85f (switch to #1)
		//
		// // on, low
		// setVolume(1, 0.3f);
		// setPitch(1, 1f); // 1 -> 1.5f (switch to #2)
		//
		// setVolume(4, 0f);
		// setPitch(4, 1.5f);
		//
		// // on, mid
		// setVolume(2, 0f);
		// setPitch(2, 1f); // 1 -> 1.33f (switch to #3)
		// setVolume(5, 0f);
		// setPitch(5, 1f);
		//
		// // on, high
		// setVolume(3, 0f);
		// setPitch(3, 1f);
		// setVolume(6, 0f);
		// setPitch(6, 1f);
	}

	@Override
	public void stop () {
		for (int i = 0; i < NumTracks; i++) {
			stop(i);
		}
	}

	@Override
	public void gameReset () {
		stop();
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

	private void start (int track) {
		if (started[track]) {
			return;
		}

		started[track] = true;
		mid[track] = loop(engine[track], 0);
		setVolume(track, 0);
	}

	private void start () {
		for (int i = 0; i < NumTracks; i++) {
			start(i);
		}
	}

	public void stop (int track) {
		if (!started[track]) {
			return;
		}

		started[track] = false;
		if (mid[track] > -1) {
			engine[track].stop(mid[track]);
		}
	}

	//

	private void setVolume (int track, float vol) {
		engine[track].setVolume(mid[track], vol);
	}

	private void setPitch (int track, float pitch) {
		engine[track].setPitch(mid[track], pitch);
	}
}
