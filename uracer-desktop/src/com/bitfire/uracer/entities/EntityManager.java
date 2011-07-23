package com.bitfire.uracer.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.Physics;

public class EntityManager
{
	private static Array<Entity> entities = new Array<Entity>();
	private static Array<SubframeInterpolableEntity> sfi_entities = new Array<SubframeInterpolableEntity>();

	public static void add( Entity ent )
	{
		entities.add( ent );
	}

	public static void add( SubframeInterpolableEntity ent )
	{
		sfi_entities.add( ent );
	}

	public static void clear()
	{
		entities.clear();
		sfi_entities.clear();
	}

	private static void raiseOnBeforePhysicsSubstep()
	{
		int len = sfi_entities.size;
		for( int i = 0; i < len; i++ )
		{
			SubframeInterpolableEntity e = sfi_entities.get( i );
			e.onBeforePhysicsSubstep();
		}
	}

	private static void raiseOnAfterPhysicsSubstep()
	{
		int len = sfi_entities.size;
		for( int i = 0; i < len; i++ )
		{
			SubframeInterpolableEntity e = sfi_entities.get( i );
			e.onAfterPhysicsSubstep();
		}
	}

	public static void raiseOnTick()
	{
		// intentionally avoid rising onTick on subframe interpolables since
		// there is already plenty of chance to tick for the fucking tickin'
		// entity

		raiseOnBeforePhysicsSubstep();
		Physics.world.step( Physics.dt, 10, 10 );
		raiseOnAfterPhysicsSubstep();

		int len = entities.size;
		for( int i = 0; i < len; i++ )
		{
			Entity e = entities.get( i );
			e.onTick();
		}
	}

	public static void raiseOnRender( SpriteBatch batch, float temporalAliasingFactor )
	{
		int len = sfi_entities.size;
		for( int i = 0; i < len; i++ )
		{
			SubframeInterpolableEntity e = sfi_entities.get( i );
			e.onBeforeRender( temporalAliasingFactor );
			e.onRender( batch, temporalAliasingFactor );
		}

		len = entities.size;
		for( int i = 0; i < len; i++ )
		{
			Entity e = entities.get( i );
			e.onRender( batch, temporalAliasingFactor );
		}
	}
}
