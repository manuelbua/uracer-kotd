package com.bitfire.uracer.entities;

import com.bitfire.uracer.events.PhysicsStepEvent;
import com.bitfire.uracer.events.PhysicsStepEvent.Type;
import com.bitfire.uracer.game.logic.PhysicsStep;

public abstract class SubframeInterpolableEntity extends Entity implements PhysicsStepEvent.Listener {
	// world-coords
	protected EntityState statePrevious = new EntityState();
	protected EntityState stateCurrent = new EntityState();

	public SubframeInterpolableEntity() {
		PhysicsStep.event.addListener( this );
	}

	public abstract void saveStateTo( EntityState state );

	public abstract boolean isSubframeInterpolated();

	protected void resetState() {
		saveStateTo( stateCurrent );
		statePrevious.set( stateCurrent );
		stateRender.set( stateCurrent );
		stateRender.toPixels();
	}

	@Override
	public void physicsEvent( Type type ) {
		switch( type ) {
		case onBeforeTimestep:
			onBeforePhysicsSubstep();
			break;
		case onAfterTimestep:
			onAfterPhysicsSubstep();
			break;
		}
	}

	public void onBeforePhysicsSubstep() {
		saveStateTo( statePrevious );
	}

	public void onAfterPhysicsSubstep() {
		saveStateTo( stateCurrent );
	}

	public void onBeforeRender( float temporalAliasingFactor ) {
		if( isSubframeInterpolated() ) {
			stateRender.set( EntityState.interpolate( statePrevious, stateCurrent, temporalAliasingFactor ) );
		} else {
			stateRender.set( stateCurrent );
		}
	}
}
