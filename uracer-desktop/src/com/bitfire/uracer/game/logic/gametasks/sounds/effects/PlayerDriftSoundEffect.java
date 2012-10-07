
package com.bitfire.uracer.game.logic.gametasks.sounds.effects;

import com.badlogic.gdx.audio.Sound;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.logic.gametasks.sounds.SoundEffect;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.player.PlayerDriftStateEvent;
import com.bitfire.uracer.game.player.PlayerDriftStateEvent.Type;
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

	private boolean doFadeIn = false;
	private boolean doFadeOut = false;
	private float lastVolume = 0f;
	private PlayerCar player;

	private PlayerDriftStateEvent.Listener driftListener = new PlayerDriftStateEvent.Listener() {
		@Override
		public void playerDriftStateEvent (PlayerCar player, Type type) {
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

	public PlayerDriftSoundEffect (PlayerCar player) {
		this.player = player;
		player.driftState.event.addListener(driftListener, PlayerDriftStateEvent.Type.onBeginDrift);
		player.driftState.event.addListener(driftListener, PlayerDriftStateEvent.Type.onEndDrift);
		drift = Sounds.carDrift;
// start();
	}

	@Override
	public void dispose () {
		player.driftState.event.removeListener(driftListener, PlayerDriftStateEvent.Type.onBeginDrift);
		player.driftState.event.removeListener(driftListener, PlayerDriftStateEvent.Type.onEndDrift);

		drift.stop();
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

	@Override
	public void start () {
		// UGLY HACK FOR ANDROID
		if (Config.isDesktop) {
			driftId = drift.loop(0f);
		} else {
			driftId = checkedLoop(drift, 0f);
		}

		drift.setPitch(driftId, pitchMin);
		drift.setVolume(driftId, 0f);
	}

	@Override
	public void stop () {
		if (driftId > -1) {
			drift.stop(driftId);
		}

		doFadeIn = false;
		doFadeOut = false;
	}

	@Override
	public void reset () {
		stop();
		lastVolume = 0;
	}

	@Override
	public void tick () {
		if (driftId > -1) {
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
			lastVolume = AMath.clamp(lastVolume, 0, 1f);
			drift.setVolume(driftId, player.driftState.driftStrength * lastVolume);
		}
	}
}
