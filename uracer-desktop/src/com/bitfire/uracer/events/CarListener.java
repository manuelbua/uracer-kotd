package com.bitfire.uracer.events;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.bitfire.uracer.carsimulation.CarForces;
import com.bitfire.uracer.entities.vehicles.Car;

public interface CarListener {
	void onCollision(Car car, Fixture other, Vector2 impulses);
	void onComputeForces(CarForces forces);
}
