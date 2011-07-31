package com.bitfire.uracer.simulation;

import com.badlogic.gdx.physics.box2d.ContactFilter;
import com.badlogic.gdx.physics.box2d.Fixture;

public class CarContactFilter implements ContactFilter
{

	@Override
	public boolean shouldCollide( Fixture fixtureA, Fixture fixtureB )
	{
		System.out.println(fixtureA);
		return false;
	}

}
