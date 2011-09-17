package com.bitfire.uracer.simulations.car;

import com.badlogic.gdx.physics.box2d.ContactFilter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.bitfire.uracer.utils.Box2DUtils;

public class CarContactFilter implements ContactFilter
{
	@Override
	public boolean shouldCollide( Fixture fixtureA, Fixture fixtureB )
	{
		return !Box2DUtils.isPlayerAgainstGhost( fixtureA, fixtureB );
	}
}
