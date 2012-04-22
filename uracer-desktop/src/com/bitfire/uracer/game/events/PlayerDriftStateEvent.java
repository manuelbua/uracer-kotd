package com.bitfire.uracer.game.events;

import com.bitfire.uracer.game.actors.PlayerCar;
import com.bitfire.uracer.utils.Event;
import com.bitfire.uracer.utils.EventListener;
import com.bitfire.uracer.utils.EventNotifier;

public final class PlayerDriftStateEvent extends Event {
	public enum Type {
		onBeginDrift, onEndDrift
	}

	public interface Listener extends EventListener {
		void driftStateEvent( PlayerCar source, Type type );
	}

	public PlayerDriftStateEvent() {
		for( Type t : Type.values() ) {
			notifiers[t.ordinal()] = new Notifier();
		}
	}

	public void addListener( Listener listener, Type type ) {
		notifiers[type.ordinal()].addListener( listener );
	}

	public void trigger( PlayerCar source, Type type ) {
		this.source = source;
		notifiers[type.ordinal()].driftStateEvent( source, type );
	}

	public PlayerCar source;
	private Notifier[] notifiers = new Notifier[ Type.values().length ];

	private class Notifier extends EventNotifier<Listener> implements Listener {
		@Override
		public void driftStateEvent( PlayerCar source, Type type ) {
			for( Listener listener : listeners ) {
				listener.driftStateEvent( source, type );
			}
		}
	};
}
