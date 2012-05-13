package com.bitfire.uracer.game.rendering;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.utils.Event;
import com.bitfire.uracer.utils.EventListener;
import com.bitfire.uracer.utils.EventNotifier;

public class GameRendererEvent extends Event<GameRenderer> {
	/** defines the type of render queue */
	public enum Type {
		OnSubframeInterpolate, BatchBeforeMeshes, BatchAfterMeshes, BatchDebug, Debug;
	}

	/** defines the position in the render queue specified by the Type parameter */
	public enum Order {
		MINUS_4, MINUS_3, MINUS_2, MINUS_1, DEFAULT, PLUS_1, PLUS_2, PLUS_3, PLUS_4;
	}

	public SpriteBatch batch;
	public float timeAliasingFactor;

	public interface Listener extends EventListener {
		void gameRendererEvent( Type type );
	}

	private class Notifier extends EventNotifier<Listener> implements Listener {
		@Override
		public void gameRendererEvent( Type type ) {
			for( Listener listener : listeners ) {
				listener.gameRendererEvent( type );
			}
		}
	};

	/* This constructor will permits late-binding of the "source" member via the "trigger" method */
	public GameRendererEvent() {
		super( null );
		for( Type t : Type.values() ) {
			for( Order o : Order.values() ) {
				notifiers[t.ordinal()][o.ordinal()] = new Notifier();
			}
		}
	}

	private Notifier[][] notifiers = new Notifier[ Type.values().length ][ Order.values().length ];

	/** Adds the specified Listener to the rendering queue identified
	 * by Type and Order: only the specified event type will trigger
	 * the event for the specified listener.
	 *
	 * @param listener the listener to be notified of the event
	 * @param type the event type
	 * @param order the order in the rendering queue for the specified event type */
	public void addListener( Listener listener, Type type, Order order ) {
		notifiers[type.ordinal()][order.ordinal()].addListener( listener );
	}

	public void removeListener( Listener listener, Type type, Order order ) {
		notifiers[type.ordinal()][order.ordinal()].removeListener( listener );
	}

	public void removeAllListeners() {
		for( Type t : Type.values() ) {
			for( Order o : Order.values() ) {
				notifiers[t.ordinal()][o.ordinal()].removeAllListeners();
			}
		}
	}

	public void trigger( GameRenderer source, Type type ) {
		this.source = source;
		for( Order order : Order.values() ) {
			notifiers[type.ordinal()][order.ordinal()].gameRendererEvent( type );
		}
	}

}
