
package com.bitfire.uracer.game.logic.gametasks.sounds.effects;

import com.badlogic.gdx.audio.Sound;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.events.PlayerDriftStateEvent;
import com.bitfire.uracer.game.events.PlayerDriftStateEvent.Order;
import com.bitfire.uracer.game.events.PlayerDriftStateEvent.Type;
import com.bitfire.uracer.game.logic.gametasks.SoundManager;
import com.bitfire.uracer.game.logic.gametasks.sounds.SoundEffect;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.resources.Sounds;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.AudioUtils;

/** Implements car drifting sound effects, modulating amplitude's volume and pitch accordingly to the car's physical behavior and
 * properties. The behavior is extrapolated from the resultant computed forces upon user input interaction with the car simulator.
 * 
 * @author bmanuel */
public final class PlayerDriftSoundEffect extends SoundEffect {
	private Sound drift = null;
	private long driftId = -1, lastDriftId = -1;
	private float driftLastPitch = 0;
	private static final float pitchFactor = 1f;
	private static final float pitchMin = 0.7f;
	private static final float pitchMax = 1f;

	private boolean started = false;
	private boolean doFadeIn = false;
	private boolean doFadeOut = false;
	private float lastVolume = 0f;

	public PlayerDriftSoundEffect () {
		drift = Sounds.carDrift;
	}

	@Override
	public void dispose () {
		detach();
		stop();
	}

	private PlayerDriftStateEvent.Listener driftListener = new PlayerDriftStateEvent.Listener() {
		@Override
		public void handle (Object source, Type type, Order order) {
			switch (type) {
			case onBeginDrift:
				onBeginDrift();
				break;
			case onEndDrift:
				onEndDrift();
				break;
			}
		}
	};

	private void attach () {
		GameEvents.driftState.addListener(driftListener, PlayerDriftStateEvent.Type.onBeginDrift);
		GameEvents.driftState.addListener(driftListener, PlayerDriftStateEvent.Type.onEndDrift);
	}

	private void detach () {
		GameEvents.driftState.removeListener(driftListener, PlayerDriftStateEvent.Type.onBeginDrift);
		GameEvents.driftState.removeListener(driftListener, PlayerDriftStateEvent.Type.onEndDrift);
	}

	@Override
	public void player (PlayerCar player) {
		super.player(player);

		if (hasPlayer) {
			attach();
			start();
		} else {
			detach();
			stop();
		}
	}

	private void onBeginDrift () {
		if (driftId > -1) {
			drift.stop(driftId);
			driftId = drift.loop(0f);
			drift.setVolume(driftId, 0f);
		}

		doFadeIn = true;
		doFadeOut = false;
	}

	public void onEndDrift () {
		doFadeIn = false;
		doFadeOut = true;
	}

	private void start () {
		if (started) {
			return;
		}

		started = true;
		driftId = loop(drift, 0f);
		drift.setPitch(driftId, pitchMin);
		drift.setVolume(driftId, 0f);
	}

	@Override
	public void stop () {
		if (!started) {
			return;
		}

		if (driftId > -1) {
			drift.stop(driftId);
		}

		doFadeIn = false;
		doFadeOut = false;
		started = false;
	}

	@Override
	public void gamePause () {
		super.gamePause();
		if (driftId > -1) {
			drift.setVolume(driftId, 0f);
		}
	}

	@Override
	public void gameResume () {
		super.gameResume();
		if (hasPlayer && driftId > -1) {
			drift.setVolume(driftId, player.driftState.driftStrength * lastVolume);
		}
	}

	@Override
	public void gameReset () {
		stop();
		lastVolume = 0;
	}

	@Override
	public void gameRestart () {
		gameReset();
		start();
	}

	@Override
	public void tick () {
		if (!isPaused && hasPlayer && driftId > -1) {
			boolean anotherDriftId = (driftId != lastDriftId);
			float speedFactor = player.carState.currSpeedFactor;

			// compute behavior
			float pitch = speedFactor * pitchFactor + pitchMin;
			pitch = AMath.clamp(pitch, pitchMin, pitchMax);
			pitch = AudioUtils.timeDilationToAudioPitch(pitch, URacer.timeMultiplier);
			// System.out.println( pitch );

			if (!AMath.equals(pitch, driftLastPitch) || anotherDriftId) {
				drift.setPitch(driftId, pitch);
				driftLastPitch = pitch;
			}

			// modulate volume
			if (doFadeIn) {
				if (lastVolume < 1f) {
					lastVolume += 0.01f;
				} else {
					lastVolume = 1f;
					doFadeIn = false;
				}
			} else if (doFadeOut) {
				if (lastVolume > 0f) {
					lastVolume -= 0.03f;
				} else {
					lastVolume = 0f;
					doFadeOut = false;
				}
			}

			lastDriftId = driftId;
			lastVolume = AMath.clamp(lastVolume, 0, 2f);
			drift.setVolume(driftId, player.driftState.driftStrength * lastVolume * 1.0f * SoundManager.SfxVolumeMul);
		}
	}
}
