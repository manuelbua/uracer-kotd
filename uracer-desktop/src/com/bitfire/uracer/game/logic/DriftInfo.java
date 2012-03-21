package com.bitfire.uracer.game.logic;

import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.utils.AMath;

public class DriftInfo {
	public float driftSeconds = 0;
	public boolean isDrifting = false;
	public boolean hasCollided = false;
	public float lateralForcesFront = 0, lateralForcesRear = 0;
	public float driftStrength;

	public long driftStartTime = 0;

	private long collisionTime;
	private float lastRear = 0, lastFront = 0;

	private static DriftInfo instance = null;
	private static GameLogic logic = null;

	private DriftInfo() {
	}

	public static void init( GameLogic logic ) {
		instance = new DriftInfo();
		DriftInfo.logic = logic;
	}

	public static DriftInfo get() {
		return instance;
	}

	public void reset() {
		lastFront = lastRear = 0;
		driftSeconds = 0;
		hasCollided = false;
		isDrifting = false;
		collisionTime = 0;
		lateralForcesFront = lateralForcesRear = 0;
		driftStrength = 0;
	}

	public static void invalidateByCollision() {
		DriftInfo drift = DriftInfo.get();
		if( !drift.isDrifting ) return;

		drift.collisionTime = System.currentTimeMillis();
		drift.isDrifting = false;
		drift.hasCollided = true;
		drift.updateDriftTimeSeconds();
		logic.getListener().onEndDrift();
	}

	public void update( Car player ) {
		float oneOnMaxGrip = 1f / player.getCarModel().max_grip;

		// lateral forces are in the range [-max_grip, max_grip]
		lateralForcesFront = lastFront = AMath.lowpass( lastFront, player.getSimulator().lateralForceFront.y, 0.2f );	// get and
																														// smooth
																														// out
		lateralForcesFront = AMath.clamp( Math.abs( lateralForcesFront ) * oneOnMaxGrip, 0f, 1f );	// normalize

		lateralForcesRear = lastRear = AMath.lowpass( lastRear, player.getSimulator().lateralForceRear.y, 0.2f );	// get and
																													// smooth out
		lateralForcesRear = AMath.clamp( Math.abs( lateralForcesRear ) * oneOnMaxGrip, 0f, 1f );	// normalize

		// compute strength
		driftStrength = AMath.fixup( (lateralForcesFront + lateralForcesRear) * 0.5f );

		if( isDrifting ) {
			// update in-drift time
			updateDriftTimeSeconds();
		}

		if( hasCollided ) {
			// ignore drifts for a couple of seconds
			if( System.currentTimeMillis() - collisionTime > 1000 ) {
				hasCollided = false;
			}
		}
		else {
			// TODO should be expressed as a percent value ref. maxvel, to scale to different max velocities
			float vel = player.getCarDescriptor().velocity_wc.len();

			if( !isDrifting ) {
				// search for onBeginDrift
				if( driftStrength > 0.4f && vel > 20 ) {
					isDrifting = true;
					hasCollided = false;
					driftStartTime = System.currentTimeMillis();
					updateDriftTimeSeconds();
					logic.getListener().onBeginDrift();
				}
			}
			else {
				// search for onEndDrift
				if( isDrifting && (driftStrength < 0.2f || vel < 15f) ) {
					isDrifting = false;
					hasCollided = false;
					logic.getListener().onEndDrift();
				}
			}
		}
	}

	private void updateDriftTimeSeconds() {
		driftSeconds = (System.currentTimeMillis() - driftStartTime) * 0.001f;
	}
}
