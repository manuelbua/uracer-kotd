
package com.bitfire.uracer.game.task;

import com.bitfire.uracer.events.TaskManagerEvent;
import com.bitfire.uracer.game.GameEvents;

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
