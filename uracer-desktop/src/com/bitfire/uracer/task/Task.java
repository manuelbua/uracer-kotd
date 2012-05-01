package com.bitfire.uracer.task;

import com.bitfire.uracer.task.TaskManagerEvent.Order;
import com.bitfire.uracer.task.TaskManagerEvent.Type;

public abstract class Task implements TaskManagerEvent.Listener {

	private Order order;

	public Task() {
		this( Order.DEFAULT );
	}

	public Task( Order order ) {
		this.order = order;
		TaskManager.event.addListener( this, TaskManagerEvent.Type.onTick, order );
	}

	public void dispose() {
		TaskManager.event.removeListener( this, TaskManagerEvent.Type.onTick, order );
	}

	protected abstract void onTick();

	@Override
	public void taskManagerEvent( Type type ) {
		switch( type ) {
		case onTick:
			onTick();
			break;
		}
	}
}
