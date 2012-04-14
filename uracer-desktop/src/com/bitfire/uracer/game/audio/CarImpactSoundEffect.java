package com.bitfire.uracer.game.audio;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.bitfire.uracer.Sounds;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.data.GameData;
import com.bitfire.uracer.game.player.Car;
import com.bitfire.uracer.game.player.Car.InputMode;
import com.bitfire.uracer.game.player.CarEvent;
import com.bitfire.uracer.game.player.CarEvent.Data;
import com.bitfire.uracer.game.player.CarEvent.Type;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.AudioUtils;

public final class CarImpactSoundEffect extends CarSoundEffect {
	private Sound soundLow1, soundLow2, soundMid1, soundMid2, soundHigh;
	private long lastSoundTimeMs = 0;
	private static final long MinElapsedBetweenSoundsMs = 500;
	private static final float MinImpactForce = 20;
	private static final float MaxImpactForce = 200;
	private static final float OneOnMaxImpactForce = 1f / MaxImpactForce;
	private static final float MaxVolume = .8f;

	// pitch modulation
	private static final float pitchFactor = 1f;
	private static final float pitchMin = 0.75f;
	private static final float pitchMax = 1f;

	private CarEvent.Listener carEvent = new CarEvent.Listener() {
		@Override
		public void carEvent( Type type, Data data ) {
			Car car = (Car)GameEvents.carEvent.source;

			switch( type ) {
			case onCollision:
				if( car.getInputMode() == InputMode.InputFromPlayer ) {
					impact( data.impulses.len(), GameData.States.playerState.currSpeedFactor );
				}
				break;
			case onComputeForces:
				break;
			}
		}
	};

	public CarImpactSoundEffect() {
		GameEvents.carEvent.addListener( carEvent );

		soundLow1 = Sounds.carImpacts[0];
		soundLow2 = Sounds.carImpacts[1];
		soundMid1 = Sounds.carImpacts[2];
		soundMid2 = Sounds.carImpacts[3];
		soundHigh = Sounds.carImpacts[4];
	}

	@Override
	public void onDispose() {
		soundLow1.stop();
		soundLow1.dispose();
		soundLow2.stop();
		soundLow2.dispose();
		soundMid1.stop();
		soundMid1.dispose();
		soundMid2.stop();
		soundMid2.dispose();
		soundHigh.stop();
		soundHigh.dispose();
	}

	// FIXME, modulate pitch while playing as CarDriftSoundEffect to handle impact also on start/end time modulation
	private void impact( float impactForce, float speedFactor ) {
		// early exit
		if( impactForce < MinImpactForce ) {
			return;
		}

		// enough time passed from last impact sound?
		long millis = System.currentTimeMillis();
		if( millis - lastSoundTimeMs >= MinElapsedBetweenSoundsMs ) {
			lastSoundTimeMs = millis;

			// avoid volumes==0, min-clamp at 20
			float clampedImpactForce = AMath.clamp( impactForce, MinImpactForce, MaxImpactForce );

			float impactFactor = clampedImpactForce * OneOnMaxImpactForce;
			float volumeFactor = 1f;

			Sound s = soundLow1;

			// decides sound
			if( impactFactor <= 0.25f ) {
				// low, vol=[0.25,0.5]
				s = (MathUtils.random( 0, 100 ) < 50 ? soundLow1 : soundLow2);
				volumeFactor = 0.25f + impactFactor;
			} else if( impactFactor > 0.25f && impactFactor < 0.75f ) {
				// mid, vol=[0.5,0.75]
				s = (MathUtils.random( 0, 100 ) < 50 ? soundMid1 : soundMid2);
				volumeFactor = 0.5f + (impactFactor - 0.25f) * 0.5f;
			} else // impactFactor >= 0.75f
			{
				// high, vol=[0.75,1]
				s = soundHigh;
				volumeFactor = 0.75f + (impactFactor - 0.75f);
			}

			long id = s.play( MaxVolume * volumeFactor );
			float pitch = speedFactor * pitchFactor + pitchMin;
			pitch = AMath.clamp( pitch, pitchMin, pitchMax );
			pitch = AudioUtils.timeDilationToAudioPitch( pitch, URacer.timeMultiplier );
			s.setPitch( id, pitch );
		}
	}

	@Override
	public void onStart() {
		// unused
	}

	@Override
	public void onStop() {
		// unused
	}

	@Override
	public void onReset() {
		// unused
	}
}
