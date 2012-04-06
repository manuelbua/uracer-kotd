package com.bitfire.uracer.task;

import com.bitfire.uracer.events.TaskManagerEvent;
import com.bitfire.uracer.events.TaskManagerEvent.Order;
import com.bitfire.uracer.events.TaskManagerEvent.Type;

public abstract class Task implements TaskManagerEvent.Listener {

	public Task() {
		this( Order.Order_0 );
	}

	public Task( Order order ) {
		TaskManager.event.addListener( this, order );
	}

	public abstract void onTick();

	@Override
	public void taskManagerEvent( Type type ) {
		switch(type) {
		case onTick:
			onTick();
			break;
		}
	}
}
