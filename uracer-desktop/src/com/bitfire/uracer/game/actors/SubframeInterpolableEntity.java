
package com.bitfire.uracer.game.actors;

import com.bitfire.uracer.entities.Entity;
import com.bitfire.uracer.entities.EntityRenderState;
import com.bitfire.uracer.events.GameRendererEvent;
import com.bitfire.uracer.events.GameRendererEvent.Order;
import com.bitfire.uracer.events.PhysicsStepEvent;
import com.bitfire.uracer.game.GameEvents;

public abstract class SubframeInterpolableEntity extends Entity implements PhysicsStepEvent.Listener, GameRendererEvent.Listener {
	// world-coords
	protected EntityRenderState statePrevious = new EntityRenderState();
	protected EntityRenderState stateCurrent = new EntityRenderState();

	public SubframeInterpolableEntity () {
		GameEvents.addPhysicsListener(this);
		GameEvents.gameRenderer.addListener(this, GameRendererEvent.Type.OnSubframeInterpolate, GameRendererEvent.Order.DEFAULT);
	}

	@Override
	public void dispose () {
		GameEvents.removePhysicsListener(this);
		GameEvents.gameRenderer.removeListener(this, GameRendererEvent.Type.OnSubframeInterpolate, GameRendererEvent.Order.DEFAULT);
	}

	public abstract boolean isVisible ();

	public abstract void saveStateTo (EntityRenderState state);

	public abstract boolean isSubframeInterpolated ();

	protected void resetState () {
		saveStateTo(stateCurrent);
		statePrevious.set(stateCurrent);
		stateRender.set(stateCurrent);
		stateRender.toPixels();
	}

	@Override
	public void gameRendererEvent (GameRendererEvent.Type type, Order order) {
		if (type == GameRendererEvent.Type.OnSubframeInterpolate) {
			onSubframeInterpolate(GameEvents.gameRenderer.timeAliasingFactor);
		}
	}

	@Override
	public void physicsEvent (PhysicsStepEvent.Type type) {
		switch (type) {
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

	public void onBeforePhysicsSubstep () {
		saveStateTo(statePrevious);
	}

	public void onAfterPhysicsSubstep () {
		saveStateTo(stateCurrent);
	}

	public void onSubstepCompleted () {

	}

	/** Issued after a tick/physicsStep but before render :P */
	public void onSubframeInterpolate (float aliasingFactor) {
		if (isSubframeInterpolated()) {
			if (!EntityRenderState.isEqual(statePrevious, stateCurrent)) {
				stateRender.set(EntityRenderState.interpolate(statePrevious, stateCurrent, aliasingFactor));
			} else {
				stateRender.set(stateCurrent);
			}
		} else {
			stateRender.set(stateCurrent);
		}

		stateRender.toPixels();
	}
}
