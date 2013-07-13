
package com.bitfire.uracer.game.logic.gametasks.sounds.effects;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.events.CarEvent;
import com.bitfire.uracer.game.events.CarEvent.Order;
import com.bitfire.uracer.game.events.CarEvent.Type;
import com.bitfire.uracer.game.logic.gametasks.sounds.SoundEffect;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.resources.Sounds;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.AudioUtils;

public final class PlayerImpactSoundEffect extends SoundEffect {
	private Sound soundLow1, soundLow2, soundMid1, soundMid2, soundHigh;
	private long lastSoundTimeMs = 0;

	private static final long MinElapsedBetweenSoundsMs = 500;
	private static final float MinImpactForce = 20f;
	// private static final float MaxVolume = 1f;
	private static final float MaxVolume = .8f;

	// pitch modulation
	private static final float pitchFactor = 1f;
	private static final float pitchMin = 0.75f;
	private static final float pitchMax = 1f;

	private CarEvent.Listener carEvent = new CarEvent.Listener() {
		@Override
		public void handle (Object source, Type type, Order order) {
			impact(GameEvents.playerCar.data.impulses.len(), ((PlayerCar)source).carState.currSpeedFactor);
		}
	};

	public PlayerImpactSoundEffect () {
		soundLow1 = Sounds.carImpacts[0];
		soundLow2 = Sounds.carImpacts[1];
		soundMid1 = Sounds.carImpacts[2];
		soundMid2 = Sounds.carImpacts[3];
		soundHigh = Sounds.carImpacts[4];
	}

	@Override
	public void dispose () {
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

	// TODO modulate pitch while playing as CarDriftSoundEffect to handle impact also on start/end time modulation
	private void impact (float impactForce, float speedFactor) {
		// early exit
		// if( speedFactor < 0.1f ) {
		if (impactForce < MinImpactForce) {
			// FIXME
			// windows + jre 1.7 keeps returning wrong values for multiple micro collisions! (something like this happens even on
			// some chinese android platforms)
			// windows + jre 1.6 is fine instead
			// Gdx.app.log("PlayerImpactSoundEffect", "(low) impactForce=" + impactForce);
			return;
		}

		// Gdx.app.log("PlayerImpactSoundEffect", "impactForce=" + impactForce);

		// enough time passed from last impact sound?
		long millis = System.currentTimeMillis();
		if (millis - lastSoundTimeMs >= MinElapsedBetweenSoundsMs) {
			lastSoundTimeMs = millis;

			// avoid volumes==0, min-clamp at 20
			float clampedImpactForce = AMath.clamp(impactForce, MinImpactForce, Config.Physics.MaxImpactForce);
			// FIXME clampedImpactForce should be (clampedImpactForce-MinImpactForce) before normalization
			float impactFactor = clampedImpactForce * Config.Physics.OneOnMaxImpactForce;
			// impactFactor = speedFactor;
			float volumeFactor = 1f;

			// Gdx.app.log(this.getClass().getSimpleName(), impactForce + " (" + clampedImpactForce + ")");

			Sound s = soundLow1;

			// decides sound
			if (impactFactor <= 0.25f) {
				// low, vol=[0.25,0.5]
				s = (MathUtils.random(0, 100) < 50 ? soundLow1 : soundLow2);
				volumeFactor = 0.25f + impactFactor;
			} else if (impactFactor > 0.25f && impactFactor < 0.75f) {
				// mid, vol=[0.5,0.75]
				s = (MathUtils.random(0, 100) < 50 ? soundMid1 : soundMid2);
				volumeFactor = 0.5f + (impactFactor - 0.25f) * 0.5f;
			} else // impactFactor >= 0.75f
			{
				// high, vol=[0.75,1]
				s = soundHigh;
				volumeFactor = 0.75f + (impactFactor - 0.75f);
			}

			// Gdx.app.log( this.getClass().getSimpleName(), volumeFactor+"" );

			long id = s.play(MaxVolume * volumeFactor);
			float pitch = speedFactor * pitchFactor + pitchMin;
			pitch = AMath.clamp(pitch, pitchMin, pitchMax);
			pitch = AudioUtils.timeDilationToAudioPitch(pitch, URacer.timeMultiplier);
			s.setPitch(id, pitch);
		}
	}

	@Override
	public void stop () {
		soundLow1.stop();
		soundLow2.stop();
		soundMid1.stop();
		soundMid2.stop();
		soundHigh.stop();
	}

	@Override
	public void gameReset () {
		stop();
	}

	@Override
	public void gameRestart () {
		stop();
	}
}
