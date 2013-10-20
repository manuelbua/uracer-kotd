
package com.bitfire.uracer.game.events;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.CarForces;

public final class CarEvent extends Event<CarEvent.Type, CarEvent.Order, CarEvent.Listener> {
	public enum Type {
		onPhysicsForcesReady, onCollision, onOutOfTrack, onBackInTrack
	}

	public enum Order {
		Default
	}

	public interface Listener extends Event.Listener<Type, Order> {
		@Override
		public void handle (Object source, Type type, Order order);
	}

	public CarEvent () {
		super(Type.class, Order.class);
	}

	public final class Data {
		public Car car;

		/** collision data */
		public Fixture other;
		public Vector2 impulses;
		public float frontRatio;

		/** computed forces data */
		public CarForces forces;

		public void setCollisionData (Fixture other, Vector2 impulses, float frontRatio) {
			this.other = other;
			this.impulses = impulses;
			this.frontRatio = frontRatio;
		}

		public void setForces (CarForces forces) {
			this.forces = forces;
		}
	}

	public final Data data = new Data();
}
