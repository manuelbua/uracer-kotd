package com.bitfire.uracer;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;

public class Physics
{
	// defines how many pixels are 1 Box2d meter
	public static float PixelsToMeter;

	// defines physics dt duration
	public static float timestepHz;
	public static float dt;

	// defines time modifier
	public static float timeMultiplier;

	public static World world;

	// for return values
	private static Vector2 ret;


	public static void create( Vector2 gravity, boolean sleepWhenPossible )
	{
		PixelsToMeter = 50.0f;
		timestepHz = 60.0f;
		dt = 1.0f / timestepHz;
		timeMultiplier = 1.0f;

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
		return v * PixelsToMeter;
	}


	public static Vector2 w2s( Vector2 v )
	{
		ret.x = v.x * PixelsToMeter;
		ret.y = v.y * PixelsToMeter;
		return ret;
	}


	// convert screen to world
	public static float s2w( float v )
	{
		return v / PixelsToMeter;
	}


	public static Vector2 s2w( Vector2 v )
	{
		ret.x = v.x / PixelsToMeter;
		ret.y = v.y / PixelsToMeter;
		return ret;
	}
}
