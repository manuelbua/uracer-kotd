package com.bitfire.uracer;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;

public class Physics
{
	public static float dt;
	public static World world;

	public static void create( Vector2 gravity, boolean sleepWhenPossible )
	{
		dt = 1.0f / Config.PhysicsTimestepHz;
		world = new World( gravity, sleepWhenPossible );
	}

	public static void dispose()
	{
		world.dispose();
	}
}
