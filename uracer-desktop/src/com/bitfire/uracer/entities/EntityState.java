package com.bitfire.uracer.entities;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Physics;

public class EntityState
{
	public float orientation = 0;
	public Vector2 position = new Vector2();
	private static EntityState result = new EntityState();

	public EntityState()
	{
		orientation = 0;
		position.x = position.y = 0;
	}

	public EntityState( EntityState state )
	{
		set( state );
	}

	public EntityState( Vector2 position, float orientation )
	{
		set( position, orientation );
	}

	public void set( EntityState state )
	{
		this.orientation = state.orientation;
		this.position.set( state.position );
	}

	public void set( Vector2 position, float orientation )
	{
		this.position.set( position );
		this.orientation = orientation;
	}

	public static EntityState interpolate( EntityState previous, EntityState current, float alpha )
	{
		result.position.set( previous.position );
		result.position.set( result.position.lerp( current.position, alpha ) );
		result.orientation = current.orientation * alpha + previous.orientation * (1 - alpha);

		return result;
	}

	public void toScreen()
	{
		this.position.x = Physics.w2s( this.position.x );
		this.position.y = Physics.w2s( this.position.y );
		this.orientation = this.orientation * MathUtils.radiansToDegrees;
	}

	public void toWorld()
	{
		this.position.x = Physics.s2w( this.position.x );
		this.position.y = Physics.s2w( this.position.y );
		this.orientation = this.orientation * MathUtils.degreesToRadians;
	}
}
