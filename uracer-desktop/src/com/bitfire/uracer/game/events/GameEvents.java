package com.bitfire.uracer.game.events;

import com.bitfire.uracer.game.actors.CarEvent;

public final class GameEvents {

	public static final GameRendererEvent gameRenderer = new GameRendererEvent();
	public static final CarStateEvent carState = new CarStateEvent();
	public static final PhysicsStepEvent physicsStep = new PhysicsStepEvent();
	public static final GameLogicEvent gameLogic = new GameLogicEvent();
	public static final CarEvent carEvent = new CarEvent();
	public static final DriftStateEvent driftState = new DriftStateEvent();

	private GameEvents() {
	}
}
