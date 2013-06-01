
package com.bitfire.uracer.game;

import com.bitfire.uracer.events.CarEvent;
import com.bitfire.uracer.events.CarStateEvent;
import com.bitfire.uracer.events.GameRendererEvent;
import com.bitfire.uracer.events.PhysicsStepEvent;
import com.bitfire.uracer.events.PlayerDriftStateEvent;
import com.bitfire.uracer.events.TaskManagerEvent;

public final class GameEvents {

	public static final GameRendererEvent gameRenderer = new GameRendererEvent();
	public static final PhysicsStepEvent physicsStep = new PhysicsStepEvent();
	public static final PlayerDriftStateEvent driftState = new PlayerDriftStateEvent();
	public static final CarEvent playerCar = new CarEvent();
	public static final CarStateEvent playerCarState = new CarStateEvent();
	public static final CarEvent ghostCars = new CarEvent();
	public static final TaskManagerEvent taskManager = new TaskManagerEvent();

	private GameEvents () {
	}
}
