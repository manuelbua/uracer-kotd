package com.bitfire.uracer.simulation;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.bitfire.uracer.entities.Car;
import com.bitfire.uracer.entities.EntityType;

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

	private void addImpactFeedback( Fixture f, ContactImpulse impulse )
	{
		if( f.getUserData() == EntityType.Car && f.getBody() != null )
		{
			Car car = (Car)f.getBody().getUserData();
			car.impactFeedback.add( impulse );
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
