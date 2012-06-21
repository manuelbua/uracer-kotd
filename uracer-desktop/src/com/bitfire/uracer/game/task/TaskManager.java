package com.bitfire.uracer.game.task;

public final class TaskManager {
	protected static final TaskManagerEvent event = new TaskManagerEvent();

	private TaskManager() {
	}

	public static void dispose() {
		event.removeAllListeners();
	}

	public static void dispatchTick() {
		event.trigger( TaskManagerEvent.Type.onTick );
	}
}
