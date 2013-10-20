
package com.bitfire.uracer.game.events;

public final class PlayerLapCompletionMonitorEvent extends
	Event<PlayerLapCompletionMonitorEvent.Type, PlayerLapCompletionMonitorEvent.Order, PlayerLapCompletionMonitorEvent.Listener> {
	public enum Type {
		onWarmUpStarted, onWarmUpCompleted, onLapStarted, onLapCompleted
	}

	public enum Order {
		MINUS_4, MINUS_3, MINUS_2, MINUS_1, DEFAULT, PLUS_1, PLUS_2, PLUS_3, PLUS_4;
	}

	public interface Listener extends Event.Listener<Type, Order> {
		@Override
		public void handle (Object source, Type type, Order order);
	}

	public PlayerLapCompletionMonitorEvent () {
		super(Type.class, Order.class);
	}
}
