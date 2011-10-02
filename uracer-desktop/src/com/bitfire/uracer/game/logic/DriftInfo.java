package com.bitfire.uracer.game.logic;

import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.utils.AMath;

public class DriftInfo
{
	public long driftTime = 0;
	public boolean isDrifting = false;
	public float lateralForcesFront = 0, lateralForcesRear = 0, lastRear, lastFront;
	public float driftStrength;

	private static DriftInfo instance = null;
	private static GameLogic logic;

	public static void init( GameLogic logic )
	{
		instance = new DriftInfo();
		DriftInfo.logic = logic;
	}

	public static DriftInfo get()
	{
		return instance;
	}

	public void update( Car player )
	{
		lastFront = lateralForcesFront;
		lastRear = lateralForcesRear;

		// filter out spurious values (at car start the latf has a one-frame-long offscale value)
		lateralForcesFront = AMath.lowpass( lastFront, player.getSimulator().lateralForceFront.y, 0.2f );
		lateralForcesRear = AMath.lowpass( lastRear, player.getSimulator().lateralForceRear.y, 0.2f );

		driftStrength = AMath.clamp(
				((Math.abs( lateralForcesFront ) + Math.abs( lateralForcesRear )) * 0.5f) / player.getCarModel().max_grip, 0, 1 );

		if( !isDrifting )
		{
			// search for onBeginDrift
			if( driftStrength > 0.4f )
			{
				isDrifting = true;
				driftTime = System.currentTimeMillis();
				logic.getListener().onBeginDrift();
			}
		} else
		{
			// search for onEndDrift
			if( isDrifting && driftStrength < 0.2f )
			{
				driftTime = System.currentTimeMillis() - driftTime;
				isDrifting = false;
				logic.getListener().onEndDrift();
			}
		}

	}
}
