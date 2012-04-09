package com.bitfire.uracer.task;

import com.bitfire.uracer.game.GameData.Events;
import com.bitfire.uracer.game.events.TaskManagerEvent;
import com.bitfire.uracer.game.events.TaskManagerEvent.Order;
import com.bitfire.uracer.game.events.TaskManagerEvent.Type;

public abstract class Task implements TaskManagerEvent.Listener {

	private Order order;
	public Task() {
		this( Order.DEFAULT );
	}

	public Task( Order order ) {
		this.order = order;
		Events.taskManager.addListener( this, order );
	}

	public void dispose() {
		Events.taskManager.removeListener( this, order );
	}

	protected abstract void onTick();

	@Override
	public void taskManagerEvent( Type type ) {
		switch(type) {
		case onTick:
			onTick();
			break;
		}
	}
}
