package com.bitfire.uracer.carsimulation;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.bitfire.uracer.audio.CarSoundManager;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.game.logic.DriftInfo;
import com.bitfire.uracer.utils.Box2DUtils;

public class CarImpactManager {
	private long lastImpactTime, prevImpactTime;

	public CarImpactManager() {
		lastImpactTime = prevImpactTime = 0;
	}

	public void impact( Contact contact, ContactImpulse impulse ) {
		Fixture a = contact.getFixtureA();
		Fixture b = contact.getFixtureB();

		addImpactFeedback( a, impulse );
		addImpactFeedback( b, impulse );

	}

	private Vector2 tmpVec2 = new Vector2();

	private void addImpactFeedback( Fixture f, ContactImpulse impulse ) {
		if( (Box2DUtils.isCar( f ) || Box2DUtils.isGhostCar( f )) && f.getBody() != null ) {
			Car car = (Car)f.getBody().getUserData();
			float[] impulses = impulse.getNormalImpulses();
			tmpVec2.set( impulses[0], impulses[1] );

			// float len2 = tmpVec2.len2();
			// if( len2 > 0 )
			{
				// float len = (float)Math.sqrt( len2 );
				float len = tmpVec2.len();
				car.addImpactFeedback( len );

				// update DriftInfo in case of collision
				if( car.getInputMode() == CarInputMode.InputFromPlayer ) {
					impactByPlayer( car, len );
				}
			}
		}
	}

	private void impactByPlayer( Car car, float impulseLength ) {
		CarSoundManager.carImpacted( impulseLength );
		DriftInfo.invalidateByCollision();

		prevImpactTime = lastImpactTime;
		lastImpactTime = System.currentTimeMillis();
	}
}
