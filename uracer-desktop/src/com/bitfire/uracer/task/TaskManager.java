package com.bitfire.uracer.task;

import com.bitfire.uracer.game.GameData.Events;
import com.bitfire.uracer.game.events.TaskManagerEvent;

public class TaskManager {
	public static void dispatchTick() {
		Events.taskManager.trigger( TaskManagerEvent.Type.onTick );
	}
}
