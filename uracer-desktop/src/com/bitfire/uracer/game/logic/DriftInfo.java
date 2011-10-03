package com.bitfire.uracer.game.logic;

import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.utils.AMath;

public class DriftInfo
{
	public float driftSeconds = 0;
	public boolean isDrifting = false;
	public boolean hasCollided = false;
	public float lateralForcesFront = 0, lateralForcesRear = 0, lastRear, lastFront;
	public float driftStrength;

	public long driftStartTime = 0;
	private long collisionTime;

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

	public void reset()
	{
		lastFront = lastRear = 0;
		driftSeconds = 0;
		hasCollided = false;
		isDrifting = false;
		collisionTime = 0;
		lateralForcesFront = lateralForcesRear = 0;
		driftStrength = 0;
	}

	public static void invalidateByCollision()
	{
		DriftInfo drift = DriftInfo.get();
		if( !drift.isDrifting ) return;

		drift.collisionTime = System.currentTimeMillis();
		drift.isDrifting = false;
		drift.hasCollided = true;
		drift.updateDriftTimeSeconds();
		logic.getListener().onEndDrift();
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

		if( isDrifting )
		{
			// update in-drift time
			updateDriftTimeSeconds();
		}

		if( hasCollided )
		{
			// ignore drifts for a couple of seconds
			if( System.currentTimeMillis() - collisionTime > 1000 )
			{
				hasCollided = false;
			}
		} else
		{
			if( !isDrifting )
			{
				// search for onBeginDrift
				if( driftStrength > 0.4f && player.getCarDescriptor().velocity_wc.len() > 20 )
				{
					isDrifting = true;
					hasCollided = false;
					driftStartTime = System.currentTimeMillis();
					updateDriftTimeSeconds();
					logic.getListener().onBeginDrift();
				}
			} else
			{
				// search for onEndDrift
				if( isDrifting && driftStrength < 0.1f )
				{
					isDrifting = false;
					hasCollided = false;
					logic.getListener().onEndDrift();
				}
			}
		}
	}

	private void updateDriftTimeSeconds()
	{
		driftSeconds = (System.currentTimeMillis() - driftStartTime) / 1000f;
	}
}
