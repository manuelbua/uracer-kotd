package com.bitfire.uracer.events;

import com.bitfire.uracer.utils.Event;
import com.bitfire.uracer.utils.EventListener;
import com.bitfire.uracer.utils.EventNotifier;

public final class PlayerStateEvent extends Event {
	public enum Type {
		onTileChanged
	}

	public interface Listener extends EventListener {
		void playerStateEvent( Type type );
	}

	public void addListener( Listener listener ) {
		notify.addListener( listener );
	}

	public void trigger( Type type ) {
		notify.playerStateEvent( type );
	}

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
