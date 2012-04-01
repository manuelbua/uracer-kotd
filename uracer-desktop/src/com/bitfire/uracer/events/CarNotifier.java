package com.bitfire.uracer.events;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.bitfire.uracer.carsimulation.CarForces;
import com.bitfire.uracer.entities.vehicles.Car;

public class CarNotifier extends EventNotifier<CarListener> implements CarListener {
	@Override
	public void onCollision( Car car, Fixture other, Vector2 normalImpulses ) {
		for( CarListener listener : listeners )
			listener.onCollision( car, other, normalImpulses );
	}

	@Override
	public void onComputeForces( CarForces forces ) {
		for( CarListener listener : listeners )
			listener.onComputeForces( forces );
	}
}
