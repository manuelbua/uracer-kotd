
package com.bitfire.uracer.game.task;

import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.events.TaskManagerEvent;

public final class TaskManager {
	public TaskManager () {
	}

	public void dispose () {
		GameEvents.taskManager.removeAllListeners();
	}

	public void dispatchEvent (TaskManagerEvent.Type eventType) {
		GameEvents.taskManager.trigger(this, eventType);
	}
}
