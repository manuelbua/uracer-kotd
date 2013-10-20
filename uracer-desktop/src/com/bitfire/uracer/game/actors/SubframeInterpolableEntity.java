
package com.bitfire.uracer.game.actors;

import com.bitfire.uracer.entities.Entity;
import com.bitfire.uracer.entities.EntityRenderState;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.events.GameRendererEvent;
import com.bitfire.uracer.game.events.PhysicsStepEvent;
import com.bitfire.uracer.game.events.GameRendererEvent.Order;

public abstract class SubframeInterpolableEntity extends Entity {
	// world-coords
	protected EntityRenderState statePrevious = new EntityRenderState();
	protected EntityRenderState stateCurrent = new EntityRenderState();

	private final PhysicsStepEvent.Listener physicsListener = new PhysicsStepEvent.Listener() {

		@Override
		public void handle (Object source, PhysicsStepEvent.Type type, PhysicsStepEvent.Order order) {
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
	};

	private final GameRendererEvent.Listener renderListener = new GameRendererEvent.Listener() {
		@Override
		public void handle (Object source, com.bitfire.uracer.game.events.GameRendererEvent.Type type, Order order) {
			if (type == GameRendererEvent.Type.SubframeInterpolate) {
				onSubframeInterpolate(GameEvents.gameRenderer.timeAliasingFactor);
			}
		}
	};

	public SubframeInterpolableEntity () {
		GameEvents.physicsStep.addListener(physicsListener, PhysicsStepEvent.Type.onBeforeTimestep);
		GameEvents.physicsStep.addListener(physicsListener, PhysicsStepEvent.Type.onAfterTimestep);
		GameEvents.physicsStep.addListener(physicsListener, PhysicsStepEvent.Type.onSubstepCompleted);
		GameEvents.gameRenderer.addListener(renderListener, GameRendererEvent.Type.SubframeInterpolate,
			GameRendererEvent.Order.DEFAULT);
	}

	@Override
	public void dispose () {
		GameEvents.physicsStep.removeListener(physicsListener, PhysicsStepEvent.Type.onBeforeTimestep);
		GameEvents.physicsStep.removeListener(physicsListener, PhysicsStepEvent.Type.onAfterTimestep);
		GameEvents.physicsStep.removeListener(physicsListener, PhysicsStepEvent.Type.onSubstepCompleted);
		GameEvents.gameRenderer.removeListener(renderListener, GameRendererEvent.Type.SubframeInterpolate,
			GameRendererEvent.Order.DEFAULT);
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
