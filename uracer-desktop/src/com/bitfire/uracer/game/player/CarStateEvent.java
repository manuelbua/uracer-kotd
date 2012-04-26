package com.bitfire.uracer.game.player;

import com.bitfire.uracer.utils.Event;
import com.bitfire.uracer.utils.EventListener;
import com.bitfire.uracer.utils.EventNotifier;

public final class CarStateEvent extends Event<CarState> {
	public enum Type {
		onTileChanged
	}

	public interface Listener extends EventListener {
		void carStateEvent( CarState source, Type type );
	}

	public CarStateEvent( CarState source ) {
		super( source );
		for( Type t : Type.values() ) {
			notifiers[t.ordinal()] = new Notifier();
		}
	}

	public void addListener( Listener listener, Type type ) {
		notifiers[type.ordinal()].addListener( listener );
	}

	public void trigger( CarState source, Type type ) {
		notifiers[type.ordinal()].carStateEvent( source, type );
	}

	private Notifier[] notifiers = new Notifier[ Type.values().length ];

	private class Notifier extends EventNotifier<Listener> implements Listener {
		@Override
		public void carStateEvent( CarState source, Type type ) {
			for( Listener listener : listeners ) {
				listener.carStateEvent( source, type );
			}
		}
	};
}