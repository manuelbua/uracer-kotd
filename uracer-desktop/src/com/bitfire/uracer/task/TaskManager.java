package com.bitfire.uracer.task;

import com.bitfire.uracer.events.TaskManagerEvent;

public class TaskManager {
	public static final TaskManagerEvent event = new TaskManagerEvent();

	public static void dispatchTick() {
		event.trigger( TaskManagerEvent.Type.onTick );
	}
}
