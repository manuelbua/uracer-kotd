package com.bitfire.uracer.simulation;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;

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

	@Override
	public void postSolve( Contact contact, ContactImpulse impulse )
	{
	}

}
