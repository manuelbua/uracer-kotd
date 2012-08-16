
package com.bitfire.uracer.game;

import com.bitfire.uracer.game.logic.PhysicsStepEvent;
import com.bitfire.uracer.game.rendering.GameRendererEvent;

public final class GameEvents {

	public static final GameRendererEvent gameRenderer = new GameRendererEvent();
	public static final PhysicsStepEvent physicsStep = new PhysicsStepEvent();

	private GameEvents () {
	}
}
