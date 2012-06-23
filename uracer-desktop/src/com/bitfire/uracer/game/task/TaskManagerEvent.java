package com.bitfire.uracer.game.task;

import com.bitfire.uracer.utils.Event;
import com.bitfire.uracer.utils.EventListener;
import com.bitfire.uracer.utils.EventNotifier;

public class TaskManagerEvent extends Event<TaskManager> {

	public enum Type {
		onTick
	}

	public enum Order {
		MINUS_4, MINUS_3, MINUS_2, MINUS_1, DEFAULT, PLUS_1, PLUS_2, PLUS_3, PLUS_4;
	}

	/* This constructor will permits late-binding of the "source" member via the "trigger" method */
	public TaskManagerEvent() {
		super( null );
		for( Type t : Type.values() ) {
			for( Order o : Order.values() ) {
				notifiers[t.ordinal()][o.ordinal()] = new Notifier();
			}
		}
	}

	protected interface Listener extends EventListener {
		void taskManagerEvent( Type type );
	}

	public void addListener( Listener listener, Type type, Order order ) {
		notifiers[type.ordinal()][order.ordinal()].addListener( listener );
	}

	public void removeListener( Listener listener, Type type, Order order ) {
		notifiers[type.ordinal()][order.ordinal()].removeListener( listener );
	}

	public void removeAllListeners() {
		for( Type t : Type.values() ) {
			for( Order o : Order.values() ) {
				notifiers[t.ordinal()][o.ordinal()].removeAllListeners();
			}
		}
	}

	public void trigger( Type type ) {
		for( Order order : Order.values() ) {
			notifiers[type.ordinal()][order.ordinal()].taskManagerEvent( type );
		}
	}

	private Notifier[][] notifiers = new Notifier[ Type.values().length ][ Order.values().length ];

	public class Notifier extends EventNotifier<Listener> implements Listener {
		@Override
		public void taskManagerEvent( Type type ) {
			for( Listener listener : listeners ) {
				listener.taskManagerEvent( type );
			}
		}
	};
}
