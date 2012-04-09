package com.bitfire.uracer.game.collisions;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.bitfire.uracer.entities.EntityType;
import com.bitfire.uracer.game.entities.Car;

/** Manages to distinguish and filter out Car-to-<entity> collisions only, raising the
 * associated events on the correct entities.
 * 
 * @author bmanuel */
public class CarImpactManager extends ImpactManager {
	@Override
	public void process( Contact contact, ContactImpulse impulse ) {
		Fixture a = contact.getFixtureA();
		Fixture b = contact.getFixtureB();

		ifCarThenCollide( a, b, impulse );
		ifCarThenCollide( b, a, impulse );
	}

	private Vector2 tmpVec2 = new Vector2();

	private void ifCarThenCollide( Fixture f, Fixture other, ContactImpulse impulse ) {
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
