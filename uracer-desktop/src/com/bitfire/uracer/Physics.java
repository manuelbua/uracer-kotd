package com.bitfire.uracer;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.simulation.CarContactFilter;
import com.bitfire.uracer.simulation.CarContactListener;

public class Physics
{
	public static float dt;
	public static World world;

	public static void create( Vector2 gravity, boolean sleepWhenPossible )
	{
		dt = 1.0f / Config.PhysicsTimestepHz;
		world = new World( gravity, sleepWhenPossible );

		world.setContactListener( new CarContactListener() );
		world.setContactFilter( new CarContactFilter() );
	}

	public static void dispose()
	{
		world.dispose();
	}
}
