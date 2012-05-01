package com.bitfire.uracer.game.logic;

import com.bitfire.uracer.utils.Event;
import com.bitfire.uracer.utils.EventListener;
import com.bitfire.uracer.utils.EventNotifier;

public final class GameLogicEvent extends Event<GameLogic> {
	public enum Type {
		onRestart, onReset
	}

	public interface Listener extends EventListener {
		void gameLogicEvent( Type type );
	}

	public GameLogicEvent( GameLogic logic ) {
		super( logic );
		for( Type t : Type.values() ) {
			notifiers[t.ordinal()] = new Notifier();
		}
	}

	public void addListener( Listener listener, Type type ) {
		notifiers[type.ordinal()].addListener( listener );
	}

	public void removeListener( Listener listener, Type type ) {
		notifiers[type.ordinal()].removeListener( listener );
	}

	public void trigger( Type type ) {
		notifiers[type.ordinal()].gameLogicEvent( type );
	}

	private Notifier[] notifiers = new Notifier[ Type.values().length ];

	private class Notifier extends EventNotifier<Listener> implements Listener {
		@Override
		public void gameLogicEvent( Type type ) {
			for( Listener listener : listeners ) {
				listener.gameLogicEvent( type );
			}
		}
	};
}
