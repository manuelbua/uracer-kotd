
package com.bitfire.uracer.game.task;

import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.events.TaskManagerEvent;
import com.bitfire.uracer.game.events.TaskManagerEvent.Order;
import com.bitfire.uracer.game.events.TaskManagerEvent.Type;

public abstract class Task implements TaskManagerEvent.Listener {
	protected boolean isPaused = false;
	private Order order;

	public Task () {
		this(Order.DEFAULT);
	}

	public Task (Order order) {
		this.order = order;
		GameEvents.taskManager.addListener(this, TaskManagerEvent.Type.onTick, order);
		GameEvents.taskManager.addListener(this, TaskManagerEvent.Type.onTickCompleted, order);
		GameEvents.taskManager.addListener(this, TaskManagerEvent.Type.onPause, order);
		GameEvents.taskManager.addListener(this, TaskManagerEvent.Type.onResume, order);
	}

	public void dispose () {
		GameEvents.taskManager.removeListener(this, TaskManagerEvent.Type.onTick, order);
		GameEvents.taskManager.removeListener(this, TaskManagerEvent.Type.onTickCompleted, order);
		GameEvents.taskManager.removeListener(this, TaskManagerEvent.Type.onPause, order);
		GameEvents.taskManager.removeListener(this, TaskManagerEvent.Type.onResume, order);
	}

	protected abstract void onTick ();

	protected void onTickCompleted () {
	}

	protected void onGamePause () {
		isPaused = true;
	}

	protected void onGameResume () {
		isPaused = false;
	}

	@Override
	public void handle (Object source, Type type, Order order) {
		switch (type) {
		case onTick:
			if (!isPaused) onTick();
			break;
		case onTickCompleted:
			if (!isPaused) onTickCompleted();
			break;
		case onPause:
			onGamePause();
			break;
		case onResume:
			onGameResume();
			break;
		}
	}
}
