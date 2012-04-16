package com.bitfire.uracer.game.events;

import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.utils.Event;
import com.bitfire.uracer.utils.EventListener;
import com.bitfire.uracer.utils.EventNotifier;

public final class DriftStateEvent extends Event {
	public enum Type {
		onBeginDrift, onEndDrift
	}

	public interface Listener extends EventListener {
		void driftStateEvent( Type type );
	}

	public DriftStateEvent() {
		for( Type t : Type.values() ) {
			notifiers[t.ordinal()] = new Notifier();
		}
	}

	public void addListener( Listener listener, Type type ) {
		notifiers[type.ordinal()].addListener( listener );
	}

	public void trigger( Car source, Type type ) {
		this.source = source;
		notifiers[type.ordinal()].driftStateEvent( type );
	}

	public Car source;
	private Notifier[] notifiers = new Notifier[ Type.values().length ];

	private class Notifier extends EventNotifier<Listener> implements Listener {
		@Override
		public void driftStateEvent( Type type ) {
			for( Listener listener : listeners ) {
				listener.driftStateEvent( type );
			}
		}
	};
}
