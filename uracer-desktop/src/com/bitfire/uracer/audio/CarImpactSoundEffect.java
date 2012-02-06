package com.bitfire.uracer.audio;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.bitfire.uracer.utils.AMath;

public class CarImpactSoundEffect implements CarSoundEffect
{
	private Sound soundLow1, soundLow2, soundMid1, soundMid2, soundHigh;
	private long lastSoundTimeMs = 0;
	private final long MinElapsedBetweenSoundsMs = 500;
	private final float MinImpactForce = 20;
	private final float MaxImpactForce = 200;
	private final float OneOnMaxImpactForce = 1f / MaxImpactForce;
	private final float MaxVolume = .8f;

	public CarImpactSoundEffect()
	{
		soundLow1 = Gdx.audio.newSound(Gdx.files.getFileHandle("data/audio/impact-2.ogg", FileType.Internal));
		soundLow2 = Gdx.audio.newSound(Gdx.files.getFileHandle("data/audio/impact-3.ogg", FileType.Internal));
		soundMid1 = Gdx.audio.newSound(Gdx.files.getFileHandle("data/audio/impact-1.ogg", FileType.Internal));
		soundMid2 = Gdx.audio.newSound(Gdx.files.getFileHandle("data/audio/impact-4.ogg", FileType.Internal));
		soundHigh = Gdx.audio.newSound(Gdx.files.getFileHandle("data/audio/impact-5.ogg", FileType.Internal));
	}

	@Override
	public void dispose()
	{
		soundLow1.dispose();
		soundLow2.dispose();
		soundMid1.dispose();
		soundMid2.dispose();
		soundHigh.dispose();
	}

	public void impact(float impactForce, float speedFactor)
	{
		// early exit
		if(impactForce<MinImpactForce)
		{
			return;
		}

		// enough time passed from last impact sound?
		long millis = System.currentTimeMillis();
		if( millis - lastSoundTimeMs >= MinElapsedBetweenSoundsMs )
		{
			lastSoundTimeMs = millis;

			// avoid volumes==0, min-clamp at 20
			float clampedImpactForce = AMath.clamp( impactForce, MinImpactForce, MaxImpactForce );

			float impactFactor = clampedImpactForce * OneOnMaxImpactForce;
			float volumeFactor = 1f;

			Sound s = soundLow1;

			// decide sound
			if( impactFactor <= 0.25f )
			{
				// low, vol=[0.25,0.5]
				s = ( MathUtils.random( 0, 100 ) < 50 ? soundLow1 : soundLow2 );
				volumeFactor = 0.25f + impactFactor;
			}
			else if( impactFactor > 0.25f && impactFactor < 0.75f )
			{
				// mid, vol=[0.5,0.75]
				s = ( MathUtils.random( 0, 100 ) < 50 ? soundMid1 : soundMid2 );
				volumeFactor = 0.5f + (impactFactor-0.25f)*0.5f;
			}
			else // impactFactor >= 0.75f
			{
				// high, vol=[0.75,1]
				s = soundHigh;
				volumeFactor = 0.75f + (impactFactor-0.75f);
			}

			s.play(MaxVolume * volumeFactor);
		}
	}

	@Override
	public void start()
	{
		// unused
	}

	@Override
	public void stop()
	{
		// unused
	}
}
