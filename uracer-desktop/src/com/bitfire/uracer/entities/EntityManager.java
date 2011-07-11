package com.bitfire.uracer.entities;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;

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

	public static void onBeforePhysicsSubstep()
	{
		int len = sfi_entities.size;
		for( int i = 0; i < len; i++ )
		{
			SubframeInterpolableEntity e = sfi_entities.get( i );
			e.onBeforePhysicsSubstep();
		}
	}

	public static void onAfterPhysicsSubstep()
	{
		int len = sfi_entities.size;
		for( int i = 0; i < len; i++ )
		{
			SubframeInterpolableEntity e = sfi_entities.get( i );
			e.onAfterPhysicsSubstep();
		}
	}

	public static void onRender(SpriteBatch batch, Camera screen, Camera world, float temporalAliasingFactor)
	{
		int len = sfi_entities.size;
		for( int i = 0; i < len; i++ )
		{
			SubframeInterpolableEntity e = sfi_entities.get( i );
			e.onBeforeRender( temporalAliasingFactor );
			e.onRender( batch, screen, world, temporalAliasingFactor );
		}

		len = entities.size;
		for( int i = 0; i < len; i++ )
		{
			Entity e = entities.get( i );
			e.onRender( batch, screen, world, temporalAliasingFactor );
		}
	}
}
