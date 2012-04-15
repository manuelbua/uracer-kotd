package com.bitfire.uracer.game;

import com.bitfire.uracer.game.actors.CarEvent;
import com.bitfire.uracer.game.events.DriftStateEvent;
import com.bitfire.uracer.game.events.GameLogicEvent;
import com.bitfire.uracer.game.events.GameRendererEvent;
import com.bitfire.uracer.game.events.PhysicsStepEvent;
import com.bitfire.uracer.game.events.PlayerStateEvent;

public final class GameEvents {

	public static final GameRendererEvent gameRenderer = new GameRendererEvent();
	public static final PlayerStateEvent playerState = new PlayerStateEvent();
	public static final PhysicsStepEvent physicsStep = new PhysicsStepEvent();
	public static final GameLogicEvent gameLogic = new GameLogicEvent();
	public static final CarEvent carEvent = new CarEvent();
	public static final DriftStateEvent playerDriftState = new DriftStateEvent();

	private GameEvents() {
	}
}
