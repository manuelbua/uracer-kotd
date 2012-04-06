package com.bitfire.uracer.events;

import com.bitfire.uracer.utils.EventListener;
import com.bitfire.uracer.utils.EventNotifier;

public class TaskManagerEvent {

	public enum Type {
		onTick
	}

	public enum Order {
		Order_Minus_4, Order_Minus_3, Order_Minus_2, Order_Minus_1, Order_0, Order_Plus_1, Order_Plus_2, Order_Plus_3, Order_Plus_4;

		private class Notifier extends EventNotifier<Listener> implements Listener {
			@Override
			public void taskManagerEvent( Type type ) {
				for( Listener listener : listeners )
					listener.taskManagerEvent( type );
			}
		};

		private final Notifier notify = new Notifier();
	}

	public interface Listener extends EventListener {
		void taskManagerEvent( Type type );
	}

	public void addListener( Listener listener, Order order ) {
		order.notify.addListener( listener );
	}

	public void trigger( Type type ) {
		for( Order order : Order.values() )
			order.notify.taskManagerEvent( type );
	}
}
