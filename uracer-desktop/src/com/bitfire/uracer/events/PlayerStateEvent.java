package com.bitfire.uracer.events;

public final class PlayerStateEvent extends Event {
	public enum EventType {
		OnTileChanged
	}

	public void addListener( PlayerStateListener listener ) {
		notify.addListener( listener );
	}

	public void trigger( EventType type ) {
		notify.playerStateEvent( type );
	}

	private final Notifier notify = new Notifier();

	private class Notifier extends EventNotifier<PlayerStateListener> implements PlayerStateListener {
		@Override
		public void playerStateEvent( EventType type ) {
			for( PlayerStateListener listener : listeners )
				listener.playerStateEvent( type );
		}
	};
}
