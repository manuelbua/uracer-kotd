package com.bitfire.uracer.game.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;

public abstract class CarSoundEffect implements ISoundEffect {
	// implements a workaround for Android, need to async-wait
	// for sound loaded but libgdx doesn't expose anything for this!

	private static final int WaitLimit = 1000;
	private static final int ThrottleMs = 100;

	protected long checkedPlay( Sound sound ) {
		return checkedPlay( sound, 1 );
	}

	protected long checkedLoop( Sound sound ) {
		return checkedLoop( sound, 1 );
	}

	protected long checkedPlay( Sound sound, float volume ) {
		int waitCounter = 0;
		long soundId = 0;

		boolean ready = false;
		while( !ready && waitCounter < WaitLimit ) {
			soundId = sound.play( volume );
			ready = (soundId == 0);
			waitCounter++;
			try {
				Thread.sleep( ThrottleMs );
				Gdx.app.log( "CarSoundEffect", "sleeping" );
			} catch( InterruptedException e ) {
			}
		}

		return soundId;
	}

	protected long checkedLoop( Sound sound, float volume ) {
		int waitCounter = 0;
		long soundId = 0;

		boolean ready = false;
		while( !ready && waitCounter < WaitLimit ) {
			soundId = sound.loop( volume );
			ready = (soundId == 0);
			waitCounter++;
			try {
				Thread.sleep( ThrottleMs );
				Gdx.app.log( "CarSoundEffect", "sleeping" );
			} catch( InterruptedException e ) {
			}
		}

		return soundId;
	}

	@Override
	public void onStart() {
	}

	@Override
	public void onStop() {
	}

	@Override
	public void onReset() {
	}

	@Override
	public void onTick() {
	}
}
