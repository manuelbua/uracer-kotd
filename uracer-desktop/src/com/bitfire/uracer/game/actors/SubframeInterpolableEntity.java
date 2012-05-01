package com.bitfire.uracer.game.actors;

import com.bitfire.uracer.entities.Entity;
import com.bitfire.uracer.entities.EntityRenderState;
import com.bitfire.uracer.game.events.GameEvents;
import com.bitfire.uracer.game.logic.PhysicsStepEvent;
import com.bitfire.uracer.game.logic.PhysicsStepEvent.Type;

public abstract class SubframeInterpolableEntity extends Entity implements PhysicsStepEvent.Listener {
	// world-coords
	protected EntityRenderState statePrevious = new EntityRenderState();
	protected EntityRenderState stateCurrent = new EntityRenderState();

	public SubframeInterpolableEntity() {
		GameEvents.physicsStep.addListener( this, PhysicsStepEvent.Type.onBeforeTimestep );
		GameEvents.physicsStep.addListener( this, PhysicsStepEvent.Type.onAfterTimestep );
		GameEvents.physicsStep.addListener( this, PhysicsStepEvent.Type.onTemporalAliasing );
	}

	@Override
	public void dispose() {
		GameEvents.physicsStep.removeListener( this, PhysicsStepEvent.Type.onBeforeTimestep );
		GameEvents.physicsStep.removeListener( this, PhysicsStepEvent.Type.onAfterTimestep );
		GameEvents.physicsStep.removeListener( this, PhysicsStepEvent.Type.onTemporalAliasing );
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
	public void physicsEvent( boolean stepped, float temporalAliasing, Type type ) {
		switch( type ) {
		case onBeforeTimestep:
			onBeforePhysicsSubstep();
			break;
		case onAfterTimestep:
			onAfterPhysicsSubstep();
			break;
		case onTemporalAliasing:
			onTemporalAliasing( stepped, temporalAliasing );
			break;
		}
	}

	public void onBeforePhysicsSubstep() {
		saveStateTo( statePrevious );
	}

	public void onAfterPhysicsSubstep() {
		saveStateTo( stateCurrent );
	}

	/** Issued after a tick/physicsStep but before render :P */
	public void onTemporalAliasing( boolean stepped, float aliasingFactor ) {
		if( isSubframeInterpolated() ) {
			stateRender.set( EntityRenderState.interpolate( statePrevious, stateCurrent, aliasingFactor ) );
		} else {
			stateRender.set( stateCurrent );
		}

		stateRender.toPixels();
	}
}
