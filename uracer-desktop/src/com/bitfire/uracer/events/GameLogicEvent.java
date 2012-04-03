package com.bitfire.uracer.events;

public final class GameLogicEvent extends Event {
	public enum EventType {
		OnRestart, OnReset
	}

	public void addListener( GameLogicListener listener ) {
		notify.addListener( listener );
	}

	public void trigger( EventType type ) {
		notify.gameLogicEvent( type );
	}

	private final Notifier notify = new Notifier();

	private class Notifier extends EventNotifier<GameLogicListener> implements GameLogicListener {
		@Override
		public void gameLogicEvent( EventType type ) {
			for( GameLogicListener listener : listeners )
				listener.gameLogicEvent( type );
		}
	};
}
