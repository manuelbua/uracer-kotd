package com.bitfire.uracer.carsimulation;

import com.badlogic.gdx.physics.box2d.ContactFilter;
import com.badlogic.gdx.physics.box2d.Fixture;

public class CarContactFilter implements ContactFilter
{
	@Override
	public boolean shouldCollide( Fixture fixtureA, Fixture fixtureB )
	{
		return false;
//		return !Box2DUtils.isPlayerAgainstGhost( fixtureA, fixtureB );
	}
}
