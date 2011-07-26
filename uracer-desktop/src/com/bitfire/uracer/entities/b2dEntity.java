package com.bitfire.uracer.entities;

import com.badlogic.gdx.physics.box2d.Body;
import com.bitfire.uracer.Config;

public class b2dEntity extends SubframeInterpolableEntity
{
	protected Body body;

	@Override
	public void saveStateTo( EntityState state )
	{
		state.position.set( body.getPosition() );
		state.orientation = body.getAngle();
	}

	@Override
	public boolean isSubframeInterpolated()
	{
		return Config.SubframeInterpolation;
	}
}
