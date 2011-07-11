package com.bitfire.uracer.entities;

import com.badlogic.gdx.math.Vector2;

public class EntityScreenState
{
	public float orientation = 0;
	public Vector2 position = new Vector2();
	private static EntityScreenState result = new EntityScreenState();

	public EntityScreenState()
	{
		orientation = 0;
		position.x = position.y = 0;
	}

	public EntityScreenState( EntityScreenState state )
	{
		set(state);
	}

	public void set( EntityScreenState state )
	{
		this.orientation = state.orientation;
		this.position.set( state.position );
	}

	public static EntityScreenState interpolate( EntityScreenState previous, EntityScreenState current, float alpha )
	{
		result.position.set( previous.position );
		result.position.set( result.position.lerp( current.position, alpha ) );
		result.orientation = current.orientation * alpha + previous.orientation * ( 1 - alpha );

		return result;
	}

}
