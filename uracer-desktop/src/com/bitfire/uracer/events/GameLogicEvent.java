package com.bitfire.uracer.events;


public final class GameLogicEvent {
	public enum EventType {
		OnRestart, OnReset
	}

	public final Notifier notify = new Notifier();

	public void addListener( GameLogicListener listener ) {
		notify.addListener( listener );
	}

	public void trigger( EventType type ) {
		notify.gameLogicEvent( type );
	}

	private class Notifier extends EventNotifier<GameLogicListener> implements GameLogicListener {
		@Override
		public void gameLogicEvent( EventType type ) {
			for( GameLogicListener listener : listeners )
				listener.gameLogicEvent( type );
		}
	};
}
