package com.bitfire.uracer.game.logic;

import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.events.DriftStateListener;
import com.bitfire.uracer.events.DriftStateNotifier;
import com.bitfire.uracer.game.GameData;
import com.bitfire.uracer.utils.AMath;

public class DriftState {
	public float driftSeconds = 0;
	public boolean isDrifting = false;
	public boolean hasCollided = false;
	public float lateralForcesFront = 0, lateralForcesRear = 0;
	public float driftStrength;

	public long driftStartTime = 0;

	private long collisionTime;
	private float lastRear = 0, lastFront = 0;

	private DriftStateNotifier notifier;

	public DriftState() {
		this.notifier = new DriftStateNotifier();
		reset();
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

	public void addListener(DriftStateListener listener) {
		notifier.addListener( listener );
	}

	// onCollision?
	public void invalidateByCollision() {
		if( !isDrifting ) return;

		collisionTime = System.currentTimeMillis();
		isDrifting = false;
		hasCollided = true;
		updateDriftTimeSeconds();
		notifier.onEndDrift();
	}

	public void tick() {
		Car car = GameData.playerState.car;
		float oneOnMaxGrip = 1f / car.getCarModel().max_grip;

		// lateral forces are in the range [-max_grip, max_grip]
		lateralForcesFront = lastFront = AMath.lowpass( lastFront, car.getSimulator().lateralForceFront.y, 0.2f );
		lateralForcesFront = AMath.clamp( Math.abs( lateralForcesFront ) * oneOnMaxGrip, 0f, 1f );	// normalize

		lateralForcesRear = lastRear = AMath.lowpass( lastRear, car.getSimulator().lateralForceRear.y, 0.2f );
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
			float vel = car.getCarDescriptor().velocity_wc.len();

			if( !isDrifting ) {
				// search for onBeginDrift
				if( driftStrength > 0.4f && vel > 20 ) {
					isDrifting = true;
					hasCollided = false;
					driftStartTime = System.currentTimeMillis();
					notifier.onBeginDrift();
				}
			}
			else {
				// search for onEndDrift
				if( isDrifting && (driftStrength < 0.2f || vel < 15f) ) {
					updateDriftTimeSeconds();
					isDrifting = false;
					hasCollided = false;
					notifier.onEndDrift();
				}
			}
		}
	}

	private void updateDriftTimeSeconds() {
//		driftSeconds = (System.currentTimeMillis() - driftStartTime) * 0.001f;
		driftSeconds = (System.currentTimeMillis() - driftStartTime) * 0.001f;

		// apply scaling
//		driftSeconds *= URacer.timeMultiplier;
	}
}
