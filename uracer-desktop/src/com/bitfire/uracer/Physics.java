package com.bitfire.uracer;

import com.badlogic.gdx.math.Vector2;

public class Physics
{
	// defines how many pixels are 1 Box2d meter
	public static float PixelsToMeter = 10.0f;

	// defines physics dt duration
	public static final float timestepHz = 60.0f;
	public static final float dt = 1.0f / timestepHz;

	// convert world coords to screen coords
	public static float w2s( float v )
	{
		return v * PixelsToMeter;
	}

	public static Vector2 w2s( Vector2 v )
	{
		v.x = v.x * PixelsToMeter;
		v.y = v.y * PixelsToMeter;
		return v;
	}


	// convert screen coords to world coords
	public static float s2w( float v )
	{
		return v / PixelsToMeter;
	}

	public static Vector2 s2w( Vector2 v )
	{
		v.x = v.x / PixelsToMeter;
		v.y = v.y / PixelsToMeter;
		return v;
	}
}
