package com.bitfire.uracer.entities;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.Convert;

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

	@Override
	public void onBeforePhysicsSubstep()
	{
		// normalize the angle (interpolation safe)
		toNormalRelativeAngle();
		super.onBeforePhysicsSubstep();
	}

	private Vector2 _pos = new Vector2();

	public Vector2 pos()
	{
		_pos.set( Convert.mt2px( body.getPosition() ) );
		return _pos;
	}

	public void pos( Vector2 pos )
	{
		setTransform( pos, orient() );
	}

	public float orient()
	{
		return -body.getAngle() * MathUtils.radiansToDegrees;
	}

	public void orient( float orient )
	{
		setTransform( pos(), orient );
	}

	private Vector2 tmp = new Vector2();
	public void setTransform( Vector2 position, float orient )
	{
		tmp.set( Convert.px2mt(position) );
		body.setTransform( tmp, -orient * MathUtils.degreesToRadians );
		toNormalRelativeAngle();
	}

	protected void toNormalRelativeAngle()
	{
		// normalize body angle since it can grow unbounded
		float angle = AMath.normalRelativeAngle(body.getAngle());
		body.setTransform( body.getPosition(), angle );
	}
}
