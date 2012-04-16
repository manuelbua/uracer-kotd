package com.bitfire.uracer.game.states;

import com.bitfire.uracer.game.Time;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.events.GameEvents;
import com.bitfire.uracer.game.events.GameLogicEvent;
import com.bitfire.uracer.game.states.DriftStateEvent.Type;
import com.bitfire.uracer.utils.AMath;

public final class DriftState {
	public Car car;
	public boolean isDrifting = false;
	public boolean hasCollided = false;
	public float lateralForcesFront = 0, lateralForcesRear = 0;
	public float driftStrength;

	private float lastRear = 0, lastFront = 0;
	private Time time, collisionTime;

	private final GameLogicEvent.Listener gameLogicEvent = new GameLogicEvent.Listener() {
		@Override
		public void gameLogicEvent( com.bitfire.uracer.game.events.GameLogicEvent.Type type ) {
			switch( type ) {
			case onReset:
			case onRestart:
				reset();
				break;
			}
		}
	};

	public DriftState( Car car ) {
		GameEvents.gameLogic.addListener( gameLogicEvent, GameLogicEvent.Type.onReset );
		GameEvents.gameLogic.addListener( gameLogicEvent, GameLogicEvent.Type.onRestart );
		this.car = car;
		reset();
	}

	// TODO, a State interface with a reset() method! this way it could be assumed the state can be bound to some other car
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
		GameEvents.driftState.trigger( this, Type.onEndDrift );
	}

	public void update() {
		float oneOnMaxGrip = 1f / car.getCarModel().max_grip;

		// lateral forces are in the range [-max_grip, max_grip]
		lateralForcesFront = AMath.lowpass( lastFront, car.getLateralForceFront().y, 0.2f );
		lastFront = lateralForcesFront;
		lateralForcesFront = AMath.clamp( Math.abs( lateralForcesFront ) * oneOnMaxGrip, 0f, 1f );	// normalize

		lateralForcesRear = AMath.lowpass( lastRear, car.getLateralForceRear().y, 0.2f );
		lastRear = lateralForcesRear;
		lateralForcesRear = AMath.clamp( Math.abs( lateralForcesRear ) * oneOnMaxGrip, 0f, 1f );	// normalize

		// compute strength
		driftStrength = AMath.fixup( (lateralForcesFront + lateralForcesRear) * 0.5f );

		if( hasCollided ) {
			// ignore drifts for a couple of seconds
			// TODO use this in a penalty system
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
					GameEvents.driftState.trigger( this, Type.onBeginDrift );
				}
			} else {
				// search for onEndDrift
				if( isDrifting && (driftStrength < 0.2f || vel < 15f) ) {
					time.stop();
					isDrifting = false;
					hasCollided = false;
					GameEvents.driftState.trigger( this, Type.onEndDrift );
				}
			}
		}
	}

	public float driftSeconds() {
		return time.elapsed( Time.Reference.Ticks );
	}
}
