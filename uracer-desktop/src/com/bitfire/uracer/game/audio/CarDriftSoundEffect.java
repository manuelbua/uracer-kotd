package com.bitfire.uracer.game.audio;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.game.GameData;
import com.bitfire.uracer.game.GameData.States;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.events.DriftStateEvent;
import com.bitfire.uracer.game.events.DriftStateEvent.Type;
import com.bitfire.uracer.utils.AMath;

/** Implements car drifting sound effects, modulating amplitude's volume and pitch
 * accordingly to the car's physical behavior and properties.
 * The behavior is extrapolated from the resultant computed forces upon user
 * input interaction with the car simulator.
 *
 * @author bmanuel */
public class CarDriftSoundEffect extends CarSoundEffect {
	private Sound drift = null;
	private long driftId = -1, lastDriftId = -1;
	private float driftLastPitch = 0;
	private final float pitchFactor = 1f;
	private final float pitchMin = 0.75f;
	private final float pitchMax = 1f;

	private boolean doFadeIn = false;
	private boolean doFadeOut = false;
	private float lastVolume = 0f;

	private DriftStateEvent.Listener driftListener = new DriftStateEvent.Listener() {
		@Override
		public void driftStateEvent( Type type ) {
			switch( type ) {
			case onBeginDrift:
				if( driftId > -1 ) {
					drift.stop( driftId );
					driftId = drift.loop( 0f );
					drift.setVolume( driftId, 0f );
				}

				doFadeIn = true;
				doFadeOut = false;
				break;
			case onEndDrift:
				doFadeIn = false;
				doFadeOut = true;
				break;
			}
		}
	};

	public CarDriftSoundEffect() {
		GameEvents.driftState.addListener( driftListener );

		// TODO. Sounds as the Art class
		drift = Gdx.audio.newSound( Gdx.files.getFileHandle( "data/audio/drift-loop.ogg", FileType.Internal ) );
	}

	@Override
	public void onDispose() {
		drift.stop();
		drift.dispose();
	}

	@Override
	public void onStart() {
		// UGLY HACK FOR ANDROID
		if( Config.isDesktop )
			driftId = drift.loop( 0f );
		else
			driftId = checkedLoop( drift, 0f );

		drift.setPitch( driftId, pitchMin );
		drift.setVolume( driftId, 0f );
	}

	@Override
	public void onStop() {
		if( driftId > -1 ) {
			drift.stop( driftId );
		}

		doFadeIn = doFadeOut = false;
	}

	@Override
	public void onReset() {
		onStop();
		lastVolume = 0;
	}

	@Override
	public void onTick() {
		if( driftId > -1 ) {
			boolean anotherDriftId = (driftId != lastDriftId);
			float speedFactor = GameData.States.playerState.currSpeedFactor;

			// compute behavior
			float pitch = speedFactor * pitchFactor + pitchMin;
			pitch = AMath.clamp( pitch, pitchMin, pitchMax );
			pitch = CarSoundManager.timeDilationToAudioPitch( pitch, URacer.timeMultiplier );
			// System.out.println( pitch );

			if( !AMath.equals( pitch, driftLastPitch ) || anotherDriftId ) {
				drift.setPitch( driftId, pitch );
				driftLastPitch = pitch;
			}

			// modulate volume
			if( doFadeIn ) {
				if( lastVolume < 1f )
					lastVolume += 0.01f;
				else {
					lastVolume = 1f;
					doFadeIn = false;
				}
			} else if( doFadeOut ) {
				if( lastVolume > 0f )
					lastVolume -= 0.03f;
				else {
					lastVolume = 0f;
					doFadeOut = false;
				}
			}

			lastDriftId = driftId;
			lastVolume = AMath.clamp( lastVolume, 0, 1f );
			drift.setVolume( driftId, States.driftState.driftStrength * lastVolume );
		}
	}
}
