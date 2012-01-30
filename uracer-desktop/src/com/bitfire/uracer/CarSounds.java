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
		drift = Gdx.audio.newSound(Gdx.files.getFileHandle("data/audio/drift-9.ogg", FileType.Internal));
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
		carEngineId = carEngine.loop(1f);
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
	private static final float pitchFactor = .95f;
	private static final float pitchMin = 0.5f;
	private static final float pitchMax = 1.25f;

	public static void drift()
	{
		if(driftId>-1) drift.stop( driftId );

		driftId = drift.play(0f);
		drift.setPitch( driftId, pitchMin );
		drift.setVolume( driftId, 0 );
	}

	private static void driftUpdate()
	{
		if( driftId > -1 )
		{
			float pitch = (1f - currSpeedFactor) * pitchFactor + pitchMin;

			pitch = AMath.clamp( pitch, pitchMin, pitchMax );

			if( !AMath.equals(pitch, driftLastPitch) )
			{
				pitch = AMath.lerp( driftLastPitch, pitch, 0.55f );

				drift.setVolume( driftId, (1f-currSpeedFactor)*0.75f );
				drift.setPitch( driftId, pitch );

				driftLastPitch = pitch;
			}
		}

	}

}
