package com.bitfire.uracer.game.events;

import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.utils.Event;
import com.bitfire.uracer.utils.EventListener;
import com.bitfire.uracer.utils.EventNotifier;

public final class PlayerCarStateEvent extends Event {
	public enum Type {
		onTileChanged
	}

	public interface Listener extends EventListener {
		void carStateEvent( Car source, Type type );
	}

	public PlayerCarStateEvent() {
		for( Type t : Type.values() ) {
			notifiers[t.ordinal()] = new Notifier();
		}
	}

	public void addListener( Listener listener, Type type ) {
		notifiers[type.ordinal()].addListener( listener );
	}

	public void trigger( Car source, Type type ) {
		notifiers[type.ordinal()].carStateEvent( source, type );
	}

	private Notifier[] notifiers = new Notifier[ Type.values().length ];

	private class Notifier extends EventNotifier<Listener> implements Listener {
		@Override
		public void carStateEvent( Car source, Type type ) {
			for( Listener listener : listeners ) {
				listener.carStateEvent( source, type );
			}
		}
	};
}
