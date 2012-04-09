package com.bitfire.uracer.game.entities;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.bitfire.uracer.carsimulation.CarForces;
import com.bitfire.uracer.utils.Event;
import com.bitfire.uracer.utils.EventListener;
import com.bitfire.uracer.utils.EventNotifier;

public final class CarEvent extends Event {
	public enum Type {
		onComputeForces, onCollision
	}

	public interface Listener extends EventListener {
		void carEvent( Type type, Data data );
	}

	public final class Data {
		/** collision data */
		public Car car;
		public Fixture other;
		public Vector2 impulses;

		/** compute forces data */
		public CarForces forces;

		public void setCollisionData( Car car, Fixture other, Vector2 impulses ) {
			this.car = car;
			this.other = other;
			this.impulses = impulses;
		}

		public void setForces( CarForces forces ) {
			this.forces = forces;
		}
	}

	public final Data data = new Data();

	public void addListener( Listener listener ) {
		notify.addListener( listener );
	}

	public void trigger( Type type ) {
		notify.carEvent( type, data );
	}

	private final Notifier notify = new Notifier();

	private class Notifier extends EventNotifier<Listener> implements Listener {
		@Override
		public void carEvent( Type type, Data data ) {
			for( Listener listener : listeners ) {
				listener.carEvent( type, data );
			}
		}
	};
}
