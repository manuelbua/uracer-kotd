package com.bitfire.uracer.audio;

import com.badlogic.gdx.audio.Sound;
import com.bitfire.uracer.utils.AMath;

public class CarEngineSoundEffect implements CarSoundEffect
{
	private Sound carEngine = null;
	private long carEngineId = -1;
	private float carEnginePitchStart = 0;
	private float carEnginePitchLast = 0;
	private final float carEnginePitchMin = 1f;

	public CarEngineSoundEffect()
	{
//		carEngine = Gdx.audio.newSound(Gdx.files.getFileHandle("data/audio/engine.ogg", FileType.Internal));
	}

	@Override
	public void dispose()
	{
//		carEngine.dispose();
	}

	public void update(float speedFactor)
	{
		if( carEngineId > -1 )
		{
			float pitch = carEnginePitchMin + speedFactor * 0.65f;
			if( !AMath.equals(pitch, carEnginePitchLast) )
			{
				carEngine.setPitch( carEngineId, pitch );
				carEnginePitchLast = pitch;
			}
		}
	}

	@Override
	public void start()
	{
		carEngineId = carEngine.loop(1f);
		carEnginePitchStart = carEnginePitchLast = carEnginePitchMin;
		carEngine.setPitch( carEngineId, carEnginePitchStart );
	}

	@Override
	public void stop()
	{
		carEngine.stop();
	}
}
