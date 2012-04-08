package com.bitfire.uracer.events;

import com.bitfire.uracer.utils.Event;
import com.bitfire.uracer.utils.EventListener;
import com.bitfire.uracer.utils.EventNotifier;

public class PhysicsStepEvent extends Event {
	public enum Type {
		onBeforeTimestep, onAfterTimestep, onTemporalAliasing
	}

	public interface Listener extends EventListener {
		void physicsEvent( Type type );
	}

	public void addListener( Listener listener ) {
		notify.addListener( listener );
	}

	public void trigger( Type type ) {
		notify.physicsEvent( type );
	}

	public float temporalAliasingFactor = 0;

	private final Notifier notify = new Notifier();

	private class Notifier extends EventNotifier<Listener> implements Listener {
		@Override
		public void physicsEvent( Type type ) {
			for( Listener listener : listeners ) {
				listener.physicsEvent( type );
			}
		}
	};
}
