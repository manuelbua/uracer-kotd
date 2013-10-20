
package com.bitfire.uracer.game.player;

import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.Time;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.events.PlayerDriftStateEvent.Type;
import com.bitfire.uracer.utils.AMath;

public final class DriftState {
	private Car car;
	public boolean isDrifting = false;
	public float lateralForcesFront = 0, lateralForcesRear = 0;
	public float driftStrength;

	private boolean hasCollided = false;
	private float lastRear = 0, lastFront = 0, invMaxGrip = 0;
	private Time time, collisionTime;

	public DriftState (Car car) {
		this.car = car;
		this.time = new Time();
		this.collisionTime = new Time();
		this.invMaxGrip = car.getCarModel().inv_max_grip;
		reset();
	}

	public void dispose () {
		time.dispose();
		collisionTime.dispose();
		GameEvents.driftState.removeAllListeners();
	}

	public void reset () {
		time.reset();
		collisionTime.reset();

		lastFront = 0;
		lastRear = 0;
		hasCollided = false;
		isDrifting = false;
		lateralForcesFront = 0;
		lateralForcesRear = 0;
		driftStrength = 0;
	}

	// onCollision?
	public void invalidateByCollision () {
		if (!isDrifting) {
			return;
		}

		isDrifting = false;
		hasCollided = true;
		collisionTime.start();
		time.stop();
		GameEvents.driftState.trigger(car, Type.onEndDrift);
	}

	public void update (float latForceFrontY, float latForceRearY, float velocityLength) {

		// lateral forces are in the range [-max_grip, max_grip]
		lateralForcesFront = AMath.lowpass(lastFront, latForceFrontY, 0.2f);
		lastFront = lateralForcesFront;
		lateralForcesFront = AMath.clamp(Math.abs(lateralForcesFront) * invMaxGrip, 0f, 1f); // normalize

		lateralForcesRear = AMath.lowpass(lastRear, latForceRearY, 0.2f);
		lastRear = lateralForcesRear;
		lateralForcesRear = AMath.clamp(Math.abs(lateralForcesRear) * invMaxGrip, 0f, 1f); // normalize

		// compute strength
		driftStrength = AMath.fixup((lateralForcesFront + lateralForcesRear) * 0.5f);

		if (hasCollided) {
			// ignore drifts for a couple of seconds
			if (collisionTime.elapsed().tickSeconds >= 0.5f) {
				collisionTime.stop();
				hasCollided = false;
			}
		} else {
			// FIXME should be expressed as a percent value ref. maxvel, to scale to different max velocities
			if (!isDrifting) {
				// search for onBeginDrift
				if (driftStrength > 0.4f && velocityLength > 20) {
					isDrifting = true;
					hasCollided = false;
					// driftStartTime = System.currentTimeMillis();
					time.start();
					GameEvents.driftState.trigger(car, Type.onBeginDrift);
					// Gdx.app.log( "DriftState", car.getClass().getSimpleName() + " onBeginDrift()" );
				}
			} else {
				// search for onEndDrift
				if (isDrifting && (driftStrength < 0.2f || velocityLength < 15f)) {
					time.stop();
					isDrifting = false;
					hasCollided = false;

					// float elapsed = time.elapsed( Time.Reference.TickSeconds );
					// Gdx.app.log( "PlayerDriftState", "playerDriftStateEvent::ds=" + NumberString.format( elapsed ) +
					// " (" + elapsed + ")" );

					GameEvents.driftState.trigger(car, Type.onEndDrift);
					// Gdx.app.log( "DriftState", car.getClass().getSimpleName() + " onEndDrift(), " + time.elapsed(
					// Time.Reference.TickSeconds ) + "s" );
				}
			}
		}
	}

	public float driftSeconds () {
		return time.elapsed().tickSeconds;
	}
}
