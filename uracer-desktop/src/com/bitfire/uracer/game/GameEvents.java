
package com.bitfire.uracer.game;

import com.bitfire.uracer.game.events.CarEvent;
import com.bitfire.uracer.game.events.GameRendererEvent;
import com.bitfire.uracer.game.events.GhostCarEvent;
import com.bitfire.uracer.game.events.LapCompletionMonitorEvent;
import com.bitfire.uracer.game.events.PhysicsStepEvent;
import com.bitfire.uracer.game.events.PlayerDriftStateEvent;
import com.bitfire.uracer.game.events.TaskManagerEvent;
import com.bitfire.uracer.game.events.WrongWayMonitorEvent;

public final class GameEvents {

	public static final GameRendererEvent gameRenderer = new GameRendererEvent();
	public static final PhysicsStepEvent physicsStep = new PhysicsStepEvent();
	public static final PlayerDriftStateEvent driftState = new PlayerDriftStateEvent();
	public static final CarEvent playerCar = new CarEvent();
	public static final GhostCarEvent ghostCars = new GhostCarEvent();
	public static final TaskManagerEvent taskManager = new TaskManagerEvent();
	public static final WrongWayMonitorEvent wrongWay = new WrongWayMonitorEvent();
	public static final LapCompletionMonitorEvent lapCompletion = new LapCompletionMonitorEvent();

	// public static final CarStateEvent playerCarState = new CarStateEvent();

	private GameEvents () {
	}
}
