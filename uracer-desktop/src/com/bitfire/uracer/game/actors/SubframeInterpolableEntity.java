package com.bitfire.uracer.game.actors;

import com.bitfire.uracer.entities.Entity;
import com.bitfire.uracer.entities.EntityRenderState;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.events.PhysicsStepEvent;
import com.bitfire.uracer.game.events.PhysicsStepEvent.Type;

public abstract class SubframeInterpolableEntity extends Entity implements PhysicsStepEvent.Listener {
	// world-coords
	protected EntityRenderState statePrevious = new EntityRenderState();
	protected EntityRenderState stateCurrent = new EntityRenderState();

	public SubframeInterpolableEntity() {
		GameEvents.physicsStep.addListener( this );
	}

	public abstract void saveStateTo( EntityRenderState state );

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
		case onTemporalAliasing:
			onTemporalAliasing( GameEvents.physicsStep.temporalAliasingFactor );
			break;
		}
	}

	public void onBeforePhysicsSubstep() {
		saveStateTo( statePrevious );
	}

	public void onAfterPhysicsSubstep() {
		saveStateTo( stateCurrent );
	}

	/** Issued after a tick/physicsStep but before render onBeforeRender :P */
	public void onTemporalAliasing(float aliasingFactor) {
		if( isSubframeInterpolated() ) {
			stateRender.set( EntityRenderState.interpolate( statePrevious, stateCurrent, aliasingFactor ) );
		} else {
			stateRender.set( stateCurrent );
		}
	}
}
