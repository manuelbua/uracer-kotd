package com.bitfire.uracer.game.actors;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.entities.Entity;
import com.bitfire.uracer.entities.EntityRenderState;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.logic.PhysicsStepEvent;
import com.bitfire.uracer.game.rendering.GameRendererEvent;
import com.bitfire.uracer.game.rendering.GameRendererEvent.Order;

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
		GameEvents.gameRenderer.removeListener( this, GameRendererEvent.Type.OnSubframeInterpolate,
				GameRendererEvent.Order.DEFAULT );
	}

	public abstract boolean isVisible();

	public abstract void onRender( SpriteBatch batch, GameRendererEvent.Type type, Order order );

	public abstract void saveStateTo( EntityRenderState state );

	public abstract boolean isSubframeInterpolated();

	protected void resetState() {
		saveStateTo( stateCurrent );
		statePrevious.set( stateCurrent );
		stateRender.set( stateCurrent );
		stateRender.toPixels();
	}

	@Override
	public void gameRendererEvent( GameRendererEvent.Type type, Order order ) {
		switch( type ) {
		case BatchBeforeMeshes:
		case BatchAfterMeshes:
			if( isVisible() ) {
				onRender( GameEvents.gameRenderer.batch, type, order );
			}
			break;
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
		// Gdx.app.log( this.getClass().getSimpleName(), "beforePhysics" );
		saveStateTo( statePrevious );
	}

	public void onAfterPhysicsSubstep() {
		// Gdx.app.log( this.getClass().getSimpleName(), "afterPhysics" );
		saveStateTo( stateCurrent );
	}

	public void onSubstepCompleted() {

	}

	/** Issued after a tick/physicsStep but before render :P */
	public void onSubframeInterpolate( float aliasingFactor ) {
		if( isSubframeInterpolated() ) {
			if( !EntityRenderState.isEqual( statePrevious, stateCurrent ) ) {
				stateRender.set( EntityRenderState.interpolate( statePrevious, stateCurrent, aliasingFactor ) );
			} else {
				stateRender.set( stateCurrent );
			}
		} else {
			stateRender.set( stateCurrent );
		}

		stateRender.toPixels();
	}
}
