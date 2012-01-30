package com.bitfire.uracer;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.bitfire.uracer.carsimulation.CarDescriptor;
import com.bitfire.uracer.carsimulation.CarInputMode;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.utils.AMath;

public class CarSounds
{
	private static Car player = null;
	private static CarDescriptor carDescriptor = null;
	private static float carMaxSpeedSq = 0;
	private static float currCarSpeedSq = 0;
	private static float currSpeedFactor = 0;
	private static float carMaxForce = 0;
	private static float currForceFactor = 0;


	public static void load()
	{
		carEngine = Gdx.audio.newSound(Gdx.files.getFileHandle("data/audio/lotus-elise.ogg", FileType.Internal));
		drift = Gdx.audio.newSound(Gdx.files.getFileHandle("data/audio/drift-loop-1.ogg", FileType.Internal));
	}

	public static void dispose()
	{
		carEngine.dispose();
		drift.dispose();
	}

	public static void setPlayer(Car player)
	{
		CarSounds.player = player;
		CarSounds.carDescriptor = player.getCarDescriptor();
		carMaxSpeedSq = carDescriptor.carModel.max_speed * carDescriptor.carModel.max_speed;
		carMaxForce = carDescriptor.carModel.max_force;
	}

	public static void tick()
	{
		if( player != null && player.getInputMode()==CarInputMode.InputFromPlayer)
		{
			// compute common factors
			currCarSpeedSq = carDescriptor.velocity_wc.len2();
			currSpeedFactor = AMath.clamp(currCarSpeedSq / carMaxSpeedSq, 0f, 1f);
			currForceFactor = AMath.clamp(carDescriptor.throttle / carMaxForce, 0f, 1f);

			carUpdate();
			driftUpdate();
		}
	}

	//
	// car engine
	//

	private static Sound carEngine = null;
	private static long carEngineId = -1;
	private static float carEnginePitchStart = 0;
	private static float carEnginePitchLast = 0;

	public static void carStart()
	{
		carEngineId = carEngine.loop(.2f);
		carEnginePitchStart = carEnginePitchLast = 1f;
		carEngine.setPitch( carEngineId, carEnginePitchStart );
	}

	public static void carStop()
	{
		carEngine.stop();
	}

	private static void carUpdate()
	{
		if( carEngineId > -1 )
		{
			float pitch = carEnginePitchStart + currSpeedFactor;
			if( !AMath.equals(pitch, carEnginePitchLast) )
			{
				carEngine.setPitch( carEngineId, pitch );
				carEnginePitchLast = pitch;
			}
		}
	}


	//
	// drift and tires
	//

	private static Sound drift = null;
	private static long driftId = -1;
	private static float driftLastPitch = 0;
	private static final float pitchFactor = 1f;
	private static final float pitchMin = 0.6f;
	private static final float pitchMax = 1f;

	private static boolean doFadeIn = false;
	private static boolean doFadeOut = false;
	private static float lastVolume = 0f;

	public static void driftPlay()
	{
		driftId = drift.loop(0f);
		drift.setPitch( driftId, pitchMin );
		drift.setVolume( driftId, 0f );
	}

	public static void driftStart()
	{
//		if(driftId>-1) drift.stop( driftId );

		doFadeIn = true;
		doFadeOut = false;
//		lastVolume = 0f;
	}

	public static void driftEnd()
	{
		doFadeIn = false;
		doFadeOut = true;
	}

	private static void driftUpdate()
	{
		if( driftId > -1 )
		{
			float pitch = ( currSpeedFactor) * pitchFactor + pitchMin;
//			float t = (( currSpeedFactor));
//			float pitch = AMath.sigmoid(t) * pitchFactor + pitchMin;

			pitch = AMath.clamp( pitch, pitchMin, pitchMax );

			if( !AMath.equals(pitch, driftLastPitch) )
			{
				pitch = AMath.lerp( driftLastPitch, pitch, 0.85f );
//				System.out.println("pitch=" + pitch);
				drift.setPitch( driftId, pitch );
				driftLastPitch = pitch;
			}

			// modulate volume
			if( doFadeIn )
			{
				if(lastVolume < 1f)
					lastVolume += 0.01f;
				else {
					lastVolume = 1f;
					doFadeIn = false;
				}
			}
			else if( doFadeOut )
			{
				if(lastVolume > 0f)
					lastVolume -= 0.01f;
				else {
					lastVolume = 0f;
					doFadeOut = false;
				}
			}

			lastVolume = AMath.clamp( lastVolume, 0, 1f );
			drift.setVolume( driftId, lastVolume );
		}

	}

}
