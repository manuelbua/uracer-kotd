package com.bitfire.uracer.game.actors;

import com.bitfire.uracer.entities.Entity;
import com.bitfire.uracer.entities.EntityRenderState;
import com.bitfire.uracer.game.events.GameEvents;
import com.bitfire.uracer.game.logic.PhysicsStepEvent;
import com.bitfire.uracer.game.rendering.GameRendererEvent;

public abstract class SubframeInterpolableEntity extends Entity implements PhysicsStepEvent.Listener, GameRendererEvent.Listener {
	// world-coords
	protected EntityRenderState statePrevious = new EntityRenderState();
	protected EntityRenderState stateCurrent = new EntityRenderState();

	public SubframeInterpolableEntity() {
		GameEvents.physicsStep.addListener( this, PhysicsStepEvent.Type.onBeforeTimestep );
		GameEvents.physicsStep.addListener( this, PhysicsStepEvent.Type.onAfterTimestep );
		GameEvents.physicsStep.addListener( this, PhysicsStepEvent.Type.onSubstepCompleted );
		GameEvents.gameRenderer.addListener( this, GameRendererEvent.Type.OnSubframeInterpolate, GameRendererEvent.Order.DEFAULT );
	}

	@Override
	public void dispose() {
		GameEvents.physicsStep.removeListener( this, PhysicsStepEvent.Type.onBeforeTimestep );
		GameEvents.physicsStep.removeListener( this, PhysicsStepEvent.Type.onAfterTimestep );
		GameEvents.physicsStep.removeListener( this, PhysicsStepEvent.Type.onSubstepCompleted );
		GameEvents.gameRenderer.removeListener( this, GameRendererEvent.Type.OnSubframeInterpolate, GameRendererEvent.Order.DEFAULT );
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
	public void gameRendererEvent( GameRendererEvent.Type type ) {
		switch( type ) {
		case OnSubframeInterpolate:
			onSubframeInterpolate( GameEvents.gameRenderer.timeAliasingFactor );
			break;
		}
	}

	@Override
	public void physicsEvent( PhysicsStepEvent.Type type ) {
		switch( type ) {
		case onBeforeTimestep:
			onBeforePhysicsSubstep();
			break;
		case onAfterTimestep:
			onAfterPhysicsSubstep();
			break;
		case onSubstepCompleted:
			onSubstepCompleted();
			break;
		}
	}

	public void onBeforePhysicsSubstep() {
		saveStateTo( statePrevious );
	}

	public void onAfterPhysicsSubstep() {
		saveStateTo( stateCurrent );
	}

	public void onSubstepCompleted() {

	}

	/** Issued after a tick/physicsStep but before render :P */
	public void onSubframeInterpolate( float aliasingFactor ) {
		if( isSubframeInterpolated() ) {
			stateRender.set( EntityRenderState.interpolate( statePrevious, stateCurrent, aliasingFactor ) );
		} else {
			stateRender.set( stateCurrent );
		}

		stateRender.toPixels();
	}
}
