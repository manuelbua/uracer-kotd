package com.bitfire.uracer.carsimulation;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.bitfire.uracer.entities.EntityType;
import com.bitfire.uracer.entities.vehicles.Car;

public class CarImpactManager {

	public CarImpactManager() {
	}

	public void impact( Contact contact, ContactImpulse impulse ) {
		Fixture a = contact.getFixtureA();
		Fixture b = contact.getFixtureB();

		addImpactFeedback( a, b, impulse );
		addImpactFeedback( b, a, impulse );

	}

	private Vector2 tmpVec2 = new Vector2();

	private void addImpactFeedback( Fixture f, Fixture other, ContactImpulse impulse ) {
		Body body = f.getBody();
		Object userData = f.getUserData();
		if( (body != null) && (userData == EntityType.Car || userData == EntityType.CarReplay) ) {
			Car car = (Car)body.getUserData();
			float[] impulses = impulse.getNormalImpulses();
			tmpVec2.set( impulses[0], impulses[1] );
			car.onCollide( other, tmpVec2 );
		}
	}
}
