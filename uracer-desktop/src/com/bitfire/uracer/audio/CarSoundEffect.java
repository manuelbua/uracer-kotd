package com.bitfire.uracer.audio;

import com.badlogic.gdx.audio.Sound;

public abstract class CarSoundEffect implements ISoundEffect
{
	// implements a workaround for Android, need to async-wait
	// for sound loaded but libgdx doesn't expose anything for this!

	private final int WaitLimit = 1000;
	private final int ThrottleMs = 100;

	protected long checkedPlay(Sound sound)
	{
		return checkedPlay(sound, 1);
	}

	protected long checkedLoop(Sound sound)
	{
		return checkedLoop(sound, 1);
	}

	protected long checkedPlay(Sound sound, float volume)
	{
		int waitCounter = 0;
		long soundId = 0;

		while((soundId = sound.play(volume)) == 0 && waitCounter < WaitLimit)
			{waitCounter++; try{Thread.sleep(ThrottleMs);}catch(InterruptedException e){}}

		return soundId;
	}

	protected long checkedLoop(Sound sound, float volume)
	{
		int waitCounter = 0;
		long soundId = 0;

		while((soundId = sound.loop(volume)) == 0 && waitCounter < WaitLimit)
			{waitCounter++; try{Thread.sleep(ThrottleMs);}catch(InterruptedException e){}}

		return soundId;
	}
}
