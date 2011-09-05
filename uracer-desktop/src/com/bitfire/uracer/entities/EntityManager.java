package com.bitfire.uracer.entities;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.Physics;

public class EntityManager
{
	private static SpriteBatch spriteBatch;
	private static Array<Entity> entities;
	private static Array<SubframeInterpolableEntity> sfi_entities;

	public static int maxSpritesInBatch()
	{
		return spriteBatch.maxSpritesInBatch;
	}

	public static int renderCalls()
	{
		return spriteBatch.renderCalls;
	}

	public static void add( Entity ent )
	{
		entities.add( ent );
	}

	public static void add( SubframeInterpolableEntity ent )
	{
		ent.resetState();
		sfi_entities.add( ent );
	}

	public static void create()
	{
		entities = new Array<Entity>();
		sfi_entities = new Array<SubframeInterpolableEntity>();
		spriteBatch = new SpriteBatch(1000, 100);
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
		// there are plenty of chances to tick already

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

	public static void raiseOnBeforeRender( float temporalAliasingFactor )
	{
		int len = sfi_entities.size;
		for( int i = 0; i < len; i++ )
		{
			SubframeInterpolableEntity e = sfi_entities.get( i );
			e.onBeforeRender( temporalAliasingFactor );
		}
	}

	public static void raiseOnRender( float temporalAliasingFactor )
	{
		OrthographicCamera cam = Director.getCamera();

		spriteBatch.setProjectionMatrix( cam.projection );
		spriteBatch.setTransformMatrix( cam.view );
		spriteBatch.begin();

		int len = sfi_entities.size;
		for( int i = 0; i < len; i++ )
		{
			SubframeInterpolableEntity e = sfi_entities.get( i );
			e.onRender( spriteBatch );
		}

		len = entities.size;
		for( int i = 0; i < len; i++ )
		{
			Entity e = entities.get( i );
			e.onRender( spriteBatch );
		}

		spriteBatch.end();
	}

	public static void raiseOnDebug()
	{
		int len = sfi_entities.size;
		for( int i = 0; i < len; i++ )
		{
			SubframeInterpolableEntity e = sfi_entities.get( i );
			e.onDebug();
		}

		len = entities.size;
		for( int i = 0; i < len; i++ )
		{
			Entity e = entities.get( i );
			e.onDebug();
		}
	}
}
