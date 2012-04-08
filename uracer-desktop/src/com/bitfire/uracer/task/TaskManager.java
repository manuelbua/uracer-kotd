package com.bitfire.uracer.task;

import com.bitfire.uracer.events.TaskManagerEvent;
import com.bitfire.uracer.game.GameData.Events;

public class TaskManager {
	public static void dispatchTick() {
		Events.taskManager.trigger( TaskManagerEvent.Type.onTick );
	}
}
