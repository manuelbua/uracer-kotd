package com.bitfire.uracer.game.audio;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.carsimulation.CarInputMode;
import com.bitfire.uracer.game.GameData.Events;
import com.bitfire.uracer.game.GameData.States;
import com.bitfire.uracer.game.actors.CarEvent;
import com.bitfire.uracer.game.actors.CarEvent.Data;
import com.bitfire.uracer.game.actors.CarEvent.Type;
import com.bitfire.uracer.utils.AMath;

public class CarImpactSoundEffect extends CarSoundEffect {
	private Sound soundLow1, soundLow2, soundMid1, soundMid2, soundHigh;
	private long lastSoundTimeMs = 0;
	private final long MinElapsedBetweenSoundsMs = 500;
	private final float MinImpactForce = 20;
	private final float MaxImpactForce = 200;
	private final float OneOnMaxImpactForce = 1f / MaxImpactForce;
	private final float MaxVolume = .8f;

	// pitch modulation
	private final float pitchFactor = 1f;
	private final float pitchMin = 0.75f;
	private final float pitchMax = 1f;

	private final CarEvent.Listener carEvent = new CarEvent.Listener() {
		@Override
		public void carEvent( Type type, Data data ) {
			switch( type ) {
			case onCollision:
				if( data.car.getInputMode() == CarInputMode.InputFromPlayer )
					impact( data.impulses.len(), States.playerState.currSpeedFactor );
				break;
			case onComputeForces:
				break;
			}
		}
	};

	public CarImpactSoundEffect() {
		Events.carEvent.addListener( carEvent );

		soundLow1 = Gdx.audio.newSound( Gdx.files.getFileHandle( "data/audio/impact-2.ogg", FileType.Internal ) );
		soundLow2 = Gdx.audio.newSound( Gdx.files.getFileHandle( "data/audio/impact-3.ogg", FileType.Internal ) );
		soundMid1 = Gdx.audio.newSound( Gdx.files.getFileHandle( "data/audio/impact-1.ogg", FileType.Internal ) );
		soundMid2 = Gdx.audio.newSound( Gdx.files.getFileHandle( "data/audio/impact-4.ogg", FileType.Internal ) );
		soundHigh = Gdx.audio.newSound( Gdx.files.getFileHandle( "data/audio/impact-5.ogg", FileType.Internal ) );
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
			pitch = CarSoundManager.timeDilationToAudioPitch( pitch, URacer.timeMultiplier );
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
