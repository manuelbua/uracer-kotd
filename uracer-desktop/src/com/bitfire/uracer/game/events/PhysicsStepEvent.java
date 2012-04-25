package com.bitfire.uracer.game.events;

import com.bitfire.uracer.utils.Event;
import com.bitfire.uracer.utils.EventListener;
import com.bitfire.uracer.utils.EventNotifier;

public class PhysicsStepEvent extends Event {
	public enum Type {
		onBeforeTimestep, onAfterTimestep, onTemporalAliasing
	}

	public interface Listener extends EventListener {
		void physicsEvent( float temporalAliasing, Type type );
	}

	public void addListener( Listener listener ) {
		notify.addListener( listener );
	}

	public void trigger( float temporalAliasing, Type type ) {
		notify.physicsEvent( temporalAliasing, type );
	}

	public float temporalAliasingFactor = 0;

	private final Notifier notify = new Notifier();

	private class Notifier extends EventNotifier<Listener> implements Listener {
		@Override
		public void physicsEvent( float temporalAliasing, Type type ) {
			for( Listener listener : listeners ) {
				listener.physicsEvent( temporalAliasing, type );
			}
		}
	};
}
