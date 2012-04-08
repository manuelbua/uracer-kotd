package com.bitfire.uracer.events;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.utils.EventListener;
import com.bitfire.uracer.utils.EventNotifier;

public class GameRendererEvent {
	/** types of render queue */
	public enum Type {
		BatchBeforeMeshes, BatchAfterMeshes, BatchDebug;
	}

	/** defines the position in the render queue specified by the Type parameter */
	public enum Order {
		Order_Minus_4, Order_Minus_3, Order_Minus_2, Order_Minus_1, Order_0, Order_Plus_1, Order_Plus_2, Order_Plus_3, Order_Plus_4;
	}

	public SpriteBatch batch;

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

	public GameRendererEvent() {
		for( Type t : Type.values() )
			for( Order o : Order.values() ) {
				notifiers[t.ordinal()][o.ordinal()] = new Notifier();
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

	public void trigger( Type type ) {
		for( Order order : Order.values() ) {
			notifiers[type.ordinal()][order.ordinal()].gameRendererEvent( type );
		}
	}

}
