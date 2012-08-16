
package com.bitfire.uracer.game;

import com.bitfire.uracer.events.GameRendererEvent;
import com.bitfire.uracer.events.PhysicsStepEvent;

public final class GameEvents {

	public static final GameRendererEvent gameRenderer = new GameRendererEvent();
	public static final PhysicsStepEvent physicsStep = new PhysicsStepEvent();

	public static void addPhysicsListener (PhysicsStepEvent.Listener listener) {
		physicsStep.addListener(listener, PhysicsStepEvent.Type.onBeforeTimestep);
		physicsStep.addListener(listener, PhysicsStepEvent.Type.onAfterTimestep);
		physicsStep.addListener(listener, PhysicsStepEvent.Type.onSubstepCompleted);
	}

	public static void removePhysicsListener (PhysicsStepEvent.Listener listener) {
		physicsStep.removeListener(listener, PhysicsStepEvent.Type.onBeforeTimestep);
		physicsStep.removeListener(listener, PhysicsStepEvent.Type.onAfterTimestep);
		physicsStep.removeListener(listener, PhysicsStepEvent.Type.onSubstepCompleted);
	}

	private GameEvents () {
	}
}
