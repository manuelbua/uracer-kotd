
package com.bitfire.uracer.game.events;

public class TaskManagerEvent extends Event<TaskManagerEvent.Type, TaskManagerEvent.Order, TaskManagerEvent.Listener> {

	public enum Type {
		onTick, onTickCompleted, onPause, onResume
	}

	public enum Order {
		MINUS_4, MINUS_3, MINUS_2, MINUS_1, DEFAULT, PLUS_1, PLUS_2, PLUS_3, PLUS_4;
	}

	public interface Listener extends Event.Listener<Type, Order> {
		@Override
		public void handle (Object source, Type type, Order order);
	}

	public TaskManagerEvent () {
		super(Type.class, Order.class);
	}
}
