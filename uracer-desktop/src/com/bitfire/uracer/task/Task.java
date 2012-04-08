package com.bitfire.uracer.task;

import com.bitfire.uracer.events.TaskManagerEvent;
import com.bitfire.uracer.events.TaskManagerEvent.Order;
import com.bitfire.uracer.events.TaskManagerEvent.Type;

public abstract class Task implements TaskManagerEvent.Listener {

	private Order order;
	public Task() {
		this( Order.Order_0 );
	}

	public Task( Order order ) {
		this.order = order;
		TaskManager.event.addListener( this, order );
	}

	public void dispose() {
		TaskManager.event.removeListener( this, order );
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
