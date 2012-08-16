
package com.bitfire.uracer.game.actors;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.bitfire.uracer.utils.Event;
import com.bitfire.uracer.utils.EventListener;
import com.bitfire.uracer.utils.EventNotifier;

public final class CarEvent extends Event<Car> {
	public enum Type {
		onComputeForces, onCollision
	}

	public interface Listener extends EventListener {
		void carEvent (Type type, Data data);
	}

	public final class Data {
		public Car car;

		/** collision data */
		public Fixture other;
		public Vector2 impulses;

		/** compute forces data */
		public CarForces forces;

		public void setCollisionData (Fixture other, Vector2 impulses) {
			this.other = other;
			this.impulses = impulses;
		}

		public void setForces (CarForces forces) {
			this.forces = forces;
		}
	}

	public final Data data = new Data();

	public CarEvent (Car car) {
		super(car);
		for (Type t : Type.values()) {
			notifiers[t.ordinal()] = new Notifier();
		}
	}

	public void addListener (Listener listener, Type type) {
		notifiers[type.ordinal()].addListener(listener);
	}

	public void removeListener (Listener listener, Type type) {
		notifiers[type.ordinal()].removeListener(listener);
	}

	public void removeAllListeners () {
		for (Type t : Type.values()) {
			notifiers[t.ordinal()].removeAllListeners();
		}
	}

	public void trigger (Car source, Type type) {
		data.car = source;
		notifiers[type.ordinal()].carEvent(type, data);
	}

	private Notifier[] notifiers = new Notifier[Type.values().length];

	private class Notifier extends EventNotifier<Listener> implements Listener {
		@Override
		public void carEvent (Type type, Data data) {
			for (Listener listener : listeners) {
				listener.carEvent(type, data);
			}
		}
	};
}
