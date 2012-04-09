package com.bitfire.uracer.task;


public final class TaskManager {
	protected static final TaskManagerEvent event = new TaskManagerEvent();

	public static void dispatchTick() {
		event.trigger( TaskManagerEvent.Type.onTick );
	}
}
