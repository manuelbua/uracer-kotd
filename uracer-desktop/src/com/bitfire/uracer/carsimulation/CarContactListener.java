package com.bitfire.uracer.carsimulation;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.game.logic.DriftInfo;
import com.bitfire.uracer.utils.Box2DUtils;

public class CarContactListener implements ContactListener
{
	@Override
	public void beginContact( Contact contact )
	{
	}

	@Override
	public void endContact( Contact contact )
	{
	}

	@Override
	public void preSolve( Contact contact, Manifold oldManifold )
	{
	}

	Vector2 tmp = new Vector2();

	private void addImpactFeedback( Fixture f, ContactImpulse impulse )
	{
		if( (Box2DUtils.isCar( f ) || Box2DUtils.isGhostCar( f )) && f.getBody() != null )
		{
			Car car = (Car)f.getBody().getUserData();
			tmp.set( impulse.getNormalImpulses()[0], impulse.getNormalImpulses()[1] );
			float res = tmp.len();

			car.addImpactFeedback( res );

			// update DriftInfo in case of collision
			if( car.getInputMode() == CarInputMode.InputFromPlayer )
			{
				DriftInfo.invalidateByCollision();
			}

			// System.out.println("Impact data =" + res );
		}
	}

	@Override
	public void postSolve( Contact contact, ContactImpulse impulse )
	{
		Fixture a = contact.getFixtureA();
		Fixture b = contact.getFixtureB();

		addImpactFeedback( a, impulse );
		addImpactFeedback( b, impulse );
	}
}
