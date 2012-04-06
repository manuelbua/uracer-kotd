package com.bitfire.uracer.game.logic;

import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.events.DriftStateEvent;
import com.bitfire.uracer.events.DriftStateEvent.Type;
import com.bitfire.uracer.game.GameData;
import com.bitfire.uracer.task.Task;
import com.bitfire.uracer.utils.AMath;

public class DriftState extends Task implements Disposable {
	public static final DriftStateEvent event = new DriftStateEvent();

	public boolean isDrifting = false;
	public boolean hasCollided = false;
	public float lateralForcesFront = 0, lateralForcesRear = 0;
	public float driftStrength;

	private float lastRear = 0, lastFront = 0;

	private Time time, collisionTime;

	public DriftState() {
		reset();
	}

	@Override
	public void dispose() {
		time = null;
	}

	public void reset() {
		time = new Time();
		collisionTime = new Time();

		lastFront = lastRear = 0;
		hasCollided = false;
		isDrifting = false;
		lateralForcesFront = lateralForcesRear = 0;
		driftStrength = 0;
	}

	// onCollision?
	public void invalidateByCollision() {
		if( !isDrifting )
			return;

		isDrifting = false;
		hasCollided = true;
		collisionTime.start();
		time.stop();
		event.trigger( Type.onEndDrift );
	}

	@Override
	public void onTick() {
		Car car = GameData.playerState.car;
		float oneOnMaxGrip = 1f / car.getCarModel().max_grip;

		// lateral forces are in the range [-max_grip, max_grip]
		lateralForcesFront = lastFront = AMath.lowpass( lastFront, car.getSimulator().lateralForceFront.y, 0.2f );
		lateralForcesFront = AMath.clamp( Math.abs( lateralForcesFront ) * oneOnMaxGrip, 0f, 1f );	// normalize

		lateralForcesRear = lastRear = AMath.lowpass( lastRear, car.getSimulator().lateralForceRear.y, 0.2f );
		lateralForcesRear = AMath.clamp( Math.abs( lateralForcesRear ) * oneOnMaxGrip, 0f, 1f );	// normalize

		// compute strength
		driftStrength = AMath.fixup( (lateralForcesFront + lateralForcesRear) * 0.5f );

		if( hasCollided ) {
			// ignore drifts for a couple of seconds
			// TODO highlight this penalty!
			if( collisionTime.elapsed( Time.Reference.Ticks ) >= 2 ) {
				collisionTime.stop();
				hasCollided = false;
			}
		} else {
			// TODO should be expressed as a percent value ref. maxvel, to scale to different max velocities
			float vel = car.getCarDescriptor().velocity_wc.len();

			if( !isDrifting ) {
				// search for onBeginDrift
				if( driftStrength > 0.4f && vel > 20 ) {
					isDrifting = true;
					hasCollided = false;
					// driftStartTime = System.currentTimeMillis();
					time.start();
					event.trigger( Type.onBeginDrift );
				}
			} else {
				// search for onEndDrift
				if( isDrifting && (driftStrength < 0.2f || vel < 15f) ) {
					time.stop();
					isDrifting = false;
					hasCollided = false;
					event.trigger( Type.onEndDrift );
				}
			}
		}
	}

	public float driftSeconds() {
		return time.elapsed( Time.Reference.Ticks );
	}
}
