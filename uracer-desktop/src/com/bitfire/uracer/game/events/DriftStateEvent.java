package com.bitfire.uracer.game.events;

import com.bitfire.uracer.game.states.DriftState;
import com.bitfire.uracer.utils.Event;
import com.bitfire.uracer.utils.EventListener;
import com.bitfire.uracer.utils.EventNotifier;

public final class DriftStateEvent extends Event {
	public enum Type {
		onBeginDrift, onEndDrift
	}

	public interface Listener extends EventListener {
		void driftStateEvent( Type type );
	}

	public void addListener( Listener listener ) {
		notify.addListener( listener );
	}

	public void trigger( DriftState driftState, Type type ) {
		this.driftState = driftState;
		notify.driftStateEvent( type );
	}

	public DriftState driftState;
	private final Notifier notify = new Notifier();

	private class Notifier extends EventNotifier<Listener> implements Listener {
		@Override
		public void driftStateEvent( Type type ) {
			for( Listener listener : listeners ) {
				listener.driftStateEvent( type );
			}
		}
	};
}
