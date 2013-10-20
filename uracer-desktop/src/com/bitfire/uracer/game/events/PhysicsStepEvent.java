
package com.bitfire.uracer.game.events;


public class PhysicsStepEvent extends Event<PhysicsStepEvent.Type, PhysicsStepEvent.Order, PhysicsStepEvent.Listener> {
	public enum Type {
		onBeforeTimestep, onAfterTimestep, onSubstepCompleted
	}

	public enum Order {
		Default
	}

	public interface Listener extends Event.Listener<Type, Order> {
		@Override
		public void handle (Object source, Type type, Order order);
	}

	public PhysicsStepEvent () {
		super(Type.class, Order.class);
	}
}
