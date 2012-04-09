package com.bitfire.uracer.task;

import com.bitfire.uracer.utils.EventListener;
import com.bitfire.uracer.utils.EventNotifier;

public class TaskManagerEvent {

	public enum Type {
		onTick
	}

	public enum Order {
		MINUS_4, MINUS_3, MINUS_2, MINUS_1, DEFAULT, PLUS_1, PLUS_2, PLUS_3, PLUS_4;

		private class Notifier extends EventNotifier<Listener> implements Listener {
			@Override
			public void taskManagerEvent( Type type ) {
				for( Listener listener : listeners ) {
					listener.taskManagerEvent( type );
				}
			}
		};

		private final Notifier notify = new Notifier();
	}

	protected interface Listener extends EventListener {
		void taskManagerEvent( Type type );
	}

	public void addListener( Listener listener, Order order ) {
		order.notify.addListener( listener );
	}

	public void removeListener( Listener listener, Order order ) {
		order.notify.removeListener( listener );
	}

	public void trigger( Type type ) {
		for( Order order : Order.values() ) {
			order.notify.taskManagerEvent( type );
		}
	}
}
