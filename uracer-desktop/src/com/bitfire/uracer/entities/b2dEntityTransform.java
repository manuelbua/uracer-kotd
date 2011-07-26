package com.bitfire.uracer.entities;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.bitfire.uracer.Physics;

public class b2dEntityTransform
{
	public Vector2 position_mt, position_px;
	public float orientation_rad, orientation_deg;

	public b2dEntityTransform()
	{
		position_mt = new Vector2();
		orientation_rad = 0f;
	}

	public b2dEntityTransform( b2dEntityTransform other )
	{
		this();
		set( other );
	}

	private void update()
	{
		position_px.set(Physics.mt2px(position_mt));
		orientation_deg = orientation_rad * MathUtils.radiansToDegrees;
	}

	public void set( b2dEntityTransform other )
	{
		this.position_mt.set( other.position_mt );
		this.orientation_rad = other.orientation_rad;
		update();
	}

	public void set( Body body )
	{
		this.position_mt.set( body.getPosition() );
		this.orientation_rad = body.getAngle();
		update();
	}
}
