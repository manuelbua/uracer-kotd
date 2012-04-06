package com.bitfire.uracer.events;

import com.bitfire.uracer.utils.Event;
import com.bitfire.uracer.utils.EventListener;
import com.bitfire.uracer.utils.EventNotifier;

public final class GameLogicEvent extends Event {
	public enum Type {
		onRestart, onReset
	}

	public interface Listener extends EventListener {
		void gameLogicEvent( Type type );
	}

	public void addListener( Listener listener ) {
		notify.addListener( listener );
	}

	public void trigger( Type type ) {
		notify.gameLogicEvent( type );
	}

	private final Notifier notify = new Notifier();

	private class Notifier extends EventNotifier<Listener> implements Listener {
		@Override
		public void gameLogicEvent( Type type ) {
			for( Listener listener : listeners )
				listener.gameLogicEvent( type );
		}
	};
}
