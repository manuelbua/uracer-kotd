package com.bitfire.uracer.game.logic;

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

	private IGameLogicListener listener = null;

	public DriftState( IGameLogicListener listener ) {
		this.listener = listener;
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

	// onCollision?
	public void invalidateByCollision() {
		if( !isDrifting ) return;

		collisionTime = System.currentTimeMillis();
		isDrifting = false;
		hasCollided = true;
		updateDriftTimeSeconds();
		listener.onEndDrift();
	}

	public void update() {
		float oneOnMaxGrip = 1f / GameData.playerState.car.getCarModel().max_grip;

		// lateral forces are in the range [-max_grip, max_grip]
		lateralForcesFront = lastFront = AMath.lowpass( lastFront, GameData.playerState.car.getSimulator().lateralForceFront.y, 0.2f );
		lateralForcesFront = AMath.clamp( Math.abs( lateralForcesFront ) * oneOnMaxGrip, 0f, 1f );	// normalize

		lateralForcesRear = lastRear = AMath.lowpass( lastRear, GameData.playerState.car.getSimulator().lateralForceRear.y, 0.2f );
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
			float vel = GameData.playerState.car.getCarDescriptor().velocity_wc.len();

			if( !isDrifting ) {
				// search for onBeginDrift
				if( driftStrength > 0.4f && vel > 20 ) {
					isDrifting = true;
					hasCollided = false;
					driftStartTime = System.currentTimeMillis();
					listener.onBeginDrift();
				}
			}
			else {
				// search for onEndDrift
				if( isDrifting && (driftStrength < 0.2f || vel < 15f) ) {
					updateDriftTimeSeconds();
					isDrifting = false;
					hasCollided = false;
					listener.onEndDrift();
				}
			}
		}
	}

	private void updateDriftTimeSeconds() {
		driftSeconds = (System.currentTimeMillis() - driftStartTime) * 0.001f;
	}
}
