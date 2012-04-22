package com.bitfire.uracer.game.actors.player;

import com.bitfire.uracer.game.Time;
import com.bitfire.uracer.game.events.PlayerDriftStateEvent;
import com.bitfire.uracer.game.events.PlayerDriftStateEvent.Type;
import com.bitfire.uracer.utils.AMath;

public final class PlayerDriftState {
	/* event */
	public final PlayerDriftStateEvent event = new PlayerDriftStateEvent();

	private PlayerCar player;
	public boolean isDrifting = false;
	public boolean hasCollided = false;
	public float lateralForcesFront = 0, lateralForcesRear = 0;
	public float driftStrength;

	private float lastRear = 0, lastFront = 0;
	private Time time, collisionTime;

	public PlayerDriftState( PlayerCar player ) {
		this.player = player;
		reset();
	}

	// TODO, a State interface with a reset() method! this way it could be assumed the state can be bound to some other
	// car
	public void reset() {
		time = new Time();
		collisionTime = new Time();

		lastFront = 0;
		lastRear = 0;
		hasCollided = false;
		isDrifting = false;
		lateralForcesFront = 0;
		lateralForcesRear = 0;
		driftStrength = 0;
	}

	// onCollision?
	public void invalidateByCollision() {
		if( !isDrifting ) {
			return;
		}

		isDrifting = false;
		hasCollided = true;
		collisionTime.start();
		time.stop();
		event.trigger( player, Type.onEndDrift );
	}

	public void update( float latForceFrontY, float latForceRearY, float velocityLength ) {
		float oneOnMaxGrip = 1f / player.getCarModel().max_grip;

		// lateral forces are in the range [-max_grip, max_grip]
		lateralForcesFront = AMath.lowpass( lastFront, latForceFrontY, 0.2f );
		lastFront = lateralForcesFront;
		lateralForcesFront = AMath.clamp( Math.abs( lateralForcesFront ) * oneOnMaxGrip, 0f, 1f );	// normalize

		lateralForcesRear = AMath.lowpass( lastRear, latForceRearY, 0.2f );
		lastRear = lateralForcesRear;
		lateralForcesRear = AMath.clamp( Math.abs( lateralForcesRear ) * oneOnMaxGrip, 0f, 1f );	// normalize

		// compute strength
		driftStrength = AMath.fixup( (lateralForcesFront + lateralForcesRear) * 0.5f );

		if( hasCollided ) {
			// ignore drifts for a couple of seconds
			// TODO use this in a penalty system
			if( collisionTime.elapsed( Time.Reference.TickSeconds ) >= 2 ) {
				collisionTime.stop();
				hasCollided = false;
			}
		} else {
			// FIXME should be expressed as a percent value ref. maxvel, to scale to different max velocities
			if( !isDrifting ) {
				// search for onBeginDrift
				if( driftStrength > 0.4f && velocityLength > 20 ) {
					isDrifting = true;
					hasCollided = false;
					// driftStartTime = System.currentTimeMillis();
					time.start();
					event.trigger( player, Type.onBeginDrift );
					// Gdx.app.log( "DriftState", car.getClass().getSimpleName() + " onBeginDrift()" );
				}
			} else {
				// search for onEndDrift
				if( isDrifting && (driftStrength < 0.2f || velocityLength < 15f) ) {
					time.stop();
					isDrifting = false;
					hasCollided = false;
					event.trigger( player, Type.onEndDrift );
					// Gdx.app.log( "DriftState", car.getClass().getSimpleName() + " onEndDrift(), " + time.elapsed(
					// Time.Reference.TickSeconds ) + "s" );
				}
			}
		}
	}

	public float driftSeconds() {
		return time.elapsed( Time.Reference.TickSeconds );
	}
}
