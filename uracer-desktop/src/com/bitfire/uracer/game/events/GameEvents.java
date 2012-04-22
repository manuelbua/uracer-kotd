package com.bitfire.uracer.game.events;

public final class GameEvents {

	public static final GameRendererEvent gameRenderer = new GameRendererEvent();
	public static final PhysicsStepEvent physicsStep = new PhysicsStepEvent();

	private GameEvents() {
	}
}
