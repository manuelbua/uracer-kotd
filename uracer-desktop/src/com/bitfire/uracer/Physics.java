package com.bitfire.uracer;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.ContactFilter;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.simulations.car.CarContactListener;

public class Physics
{
	public static float dt;
	public static World world;
	public static ContactFilter contactFilter;
	public static ContactListener contactListener;

	public static void create( Vector2 gravity, boolean sleepWhenPossible )
	{
		dt = 1.0f / Config.PhysicsTimestepHz;
		world = new World( gravity, sleepWhenPossible );

		contactListener = new CarContactListener();

		world.setContactListener( contactListener );
//		world.setContactFilter( contactFilter  );
//		world.setContinuousPhysics( true );
	}

	public static void dispose()
	{
		world.dispose();
	}
}
