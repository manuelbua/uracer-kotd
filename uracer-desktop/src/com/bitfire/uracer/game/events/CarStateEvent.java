package com.bitfire.uracer.game.events;

import com.bitfire.uracer.game.states.CarState;
import com.bitfire.uracer.utils.Event;
import com.bitfire.uracer.utils.EventListener;
import com.bitfire.uracer.utils.EventNotifier;

// FIXME, add support for notifiers instead..
public final class CarStateEvent extends Event {
	public enum Type {
		onTileChanged
	}

	public interface Listener extends EventListener {
		void playerStateEvent( Type type );
	}

	public void addListener( Listener listener ) {
		notify.addListener( listener );
	}

	public void trigger( CarState carState, Type type ) {
		this.source = carState;
		notify.playerStateEvent( type );
	}

	public CarState source;
	private final Notifier notify = new Notifier();

	private class Notifier extends EventNotifier<Listener> implements Listener {
		@Override
		public void playerStateEvent( Type type ) {
			for( Listener listener : listeners ) {
				listener.playerStateEvent( type );
			}
		}
	};
}
