
package com.bitfire.uracer.game.events;

public final class GhostLapCompletionMonitorEvent extends
	Event<GhostLapCompletionMonitorEvent.Type, GhostLapCompletionMonitorEvent.Order, GhostLapCompletionMonitorEvent.Listener> {
	public enum Type {
		onLapCompleted
	}

	public enum Order {
		MINUS_4, MINUS_3, MINUS_2, MINUS_1, DEFAULT, PLUS_1, PLUS_2, PLUS_3, PLUS_4;
	}

	public interface Listener extends Event.Listener<Type, Order> {
		@Override
		public void handle (Object source, Type type, Order order);
	}

	public GhostLapCompletionMonitorEvent () {
		super(Type.class, Order.class);
	}
}
