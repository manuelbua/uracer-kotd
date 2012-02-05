package com.bitfire.uracer.audio;

import com.bitfire.uracer.carsimulation.CarDescriptor;
import com.bitfire.uracer.carsimulation.CarInputMode;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.utils.AMath;

public class CarSoundManager
{
	private static Car player = null;
	private static CarDescriptor carDescriptor = null;

	// common sound factors
	private static float carMaxSpeedSquared = 0;
	private static float carMaxForce = 0;
	private static float currCarSpeedSquared = 0;
	private static float currSpeedFactor = 0;
	private static float currForceFactor = 0;

	// sound effects
	private static CarDriftSoundEffect carDrift;
	private static CarEngineSoundEffect carEngine;

	public static void load()
	{
		carEngine = new CarEngineSoundEffect();
		carEngine.start();

		carDrift = new CarDriftSoundEffect();
		carDrift.start();
	}

	public static void dispose()
	{
		carEngine.dispose();
		carDrift.dispose();
	}

	public static void setPlayer(Car player)
	{
		CarSoundManager.player = player;
		CarSoundManager.carDescriptor = player.getCarDescriptor();
		carMaxSpeedSquared = carDescriptor.carModel.max_speed * carDescriptor.carModel.max_speed;
		carMaxForce = carDescriptor.carModel.max_force;
	}

	public static void tick()
	{
		if( player != null && player.getInputMode()==CarInputMode.InputFromPlayer)
		{
			// compute common factors
			currCarSpeedSquared = carDescriptor.velocity_wc.len2();
			currSpeedFactor = AMath.clamp(currCarSpeedSquared / carMaxSpeedSquared, 0f, 1f);
			currForceFactor = AMath.clamp(carDescriptor.throttle / carMaxForce, 0f, 1f);

			carEngine.update( currSpeedFactor );
			carDrift.update( currSpeedFactor );
		}
	}


	//
	// drift
	//

	public static void driftBegin()
	{
		carDrift.driftBegin();
	}

	public static void driftEnd()
	{
		carDrift.driftEnd();
	}

}
