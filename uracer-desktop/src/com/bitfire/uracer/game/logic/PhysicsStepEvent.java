package com.bitfire.uracer.game.logic;

import com.bitfire.uracer.utils.Event;
import com.bitfire.uracer.utils.EventListener;
import com.bitfire.uracer.utils.EventNotifier;

public class PhysicsStepEvent extends Event<PhysicsStep> {
	public enum Type {
		onBeforeTimestep, onAfterTimestep, onTemporalAliasing
	}

	public interface Listener extends EventListener {
		void physicsEvent( boolean stepped, float temporalAliasing, Type type );
	}

	/* This constructor will permits late-binding of the "source" member via the "trigger" method */
	public PhysicsStepEvent() {
		super( null );
		for( Type t : Type.values() ) {
			notifiers[t.ordinal()] = new Notifier();
		}
	}

	public void addListener( Listener listener, Type type ) {
		notifiers[type.ordinal()].addListener( listener );
	}

	public void trigger( PhysicsStep source, boolean stepped, float temporalAliasing, Type type ) {
		this.source = source;
		notifiers[type.ordinal()].physicsEvent( stepped, temporalAliasing, type );
	}

	public float temporalAliasingFactor = 0;

	private Notifier[] notifiers = new Notifier[ Type.values().length ];

	private class Notifier extends EventNotifier<Listener> implements Listener {
		@Override
		public void physicsEvent( boolean stepped, float temporalAliasing, Type type ) {
			for( Listener listener : listeners ) {
				listener.physicsEvent( stepped, temporalAliasing, type );
			}
		}
	};
}
