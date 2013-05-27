
package com.bitfire.uracer.game.task;

import com.bitfire.uracer.events.TaskManagerEvent;

public final class TaskManager {
	protected static final TaskManagerEvent event = new TaskManagerEvent();

	private TaskManager () {
	}

	public static void dispose () {
		event.removeAllListeners();
	}

	public static void dispatchEvent (TaskManagerEvent.Type eventType) {
		event.trigger(eventType);
	}
}
