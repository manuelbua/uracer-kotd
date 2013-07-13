
package com.bitfire.uracer.game.logic.gametasks.sounds.effects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.events.PlayerLapCompletionMonitorEvent;
import com.bitfire.uracer.game.events.PlayerLapCompletionMonitorEvent.Order;
import com.bitfire.uracer.game.events.PlayerLapCompletionMonitorEvent.Type;
import com.bitfire.uracer.game.logic.gametasks.sounds.SoundEffect;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.resources.Sounds;

public final class PlayerTensiveMusic extends SoundEffect {
	private Sound music;
	private long mid;
	private boolean started, paused;
	private float lastVolume = 0f;
	private final float MinVolume = 0.5f;
	private final float VolumeRange = 1 - MinVolume;

	public PlayerTensiveMusic () {
		music = Sounds.musTensive[0];
		started = false;
		paused = false;
	}

	@Override
	public void dispose () {
		detach();
		stop();
	}

	private PlayerLapCompletionMonitorEvent.Listener playerCompletionListener = new PlayerLapCompletionMonitorEvent.Listener() {
		@Override
		public void handle (Object source, Type type, Order order) {
			start();
		}
	};

	private void attach () {
		GameEvents.lapCompletion.addListener(playerCompletionListener, PlayerLapCompletionMonitorEvent.Type.onWarmUpCompleted);
	}

	private void detach () {
		GameEvents.lapCompletion.removeListener(playerCompletionListener, PlayerLapCompletionMonitorEvent.Type.onWarmUpCompleted);
	}

	@Override
	public void player (PlayerCar player) {
		super.player(player);

		if (hasPlayer) {
			attach();
		} else {
			detach();
			stop();
		}
	}

	private void start () {
		if (started) {
			return;
		}

		started = true;
		mid = loop(music, 0f);
		music.setVolume(mid, MinVolume);
	}

	@Override
	public void stop () {
		if (!started) {
			return;
		}

		started = false;
		if (mid > -1) {
			music.stop(mid);
		}
	}

	@Override
	public void gamePause () {
		// paused = true;
	}

	@Override
	public void gameResume () {
		// paused = false;
	}

	@Override
	public void gameReset () {
		stop();
	}

	@Override
	public void gameRestart () {
		stop();
	}

	@Override
	public void tick () {
		if (hasPlayer && mid > -1 && started) {
			if (!paused) {
				lastVolume = MinVolume + VolumeRange * player.carState.currSpeedFactor;
				Gdx.app.log("", "vol=" + lastVolume);
				music.setVolume(mid, lastVolume);
			} else {
				music.setVolume(mid, 0f);
			}
		}
	}

}
