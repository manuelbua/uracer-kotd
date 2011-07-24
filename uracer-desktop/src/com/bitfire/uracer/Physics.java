package com.bitfire.uracer;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;

public class Physics
{
	public static float dt;
	public static World world;

	// for return values
	private static Vector2 ret;

	public static void create( Vector2 gravity, boolean sleepWhenPossible )
	{
		dt = 1.0f / Config.PhysicsTimestepHz;

		world = new World( gravity, sleepWhenPossible );
		ret = new Vector2();
	}

	public static void dispose()
	{
		world.dispose();
	}

	// convert world to screen
	public static float w2s( float v )
	{
		return v * Config.PixelsPerMeter;
	}

	public static Vector2 w2s( Vector2 v )
	{
		ret.x = v.x * Config.PixelsPerMeter;
		ret.y = v.y * Config.PixelsPerMeter;
		return ret;
	}

	// convert screen to world
	public static float s2w( float v )
	{
		return v / Config.PixelsPerMeter;
	}

	public static Vector2 s2w( Vector2 v )
	{
		ret.x = v.x / Config.PixelsPerMeter;
		ret.y = v.y / Config.PixelsPerMeter;
		return ret;
	}
}
