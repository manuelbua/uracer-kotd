package com.bitfire.uracer;

public class Config
{

	public static boolean SubframeInterpolation;
	public static float PixelsToMeter;
	public static float PhysicsTimestepHz;
	public static float PhysicsTimeMultiplier;

	public static void asDefault()
	{
		SubframeInterpolation = true;
		PixelsToMeter = 50.0f;
		PhysicsTimestepHz = 60.0f;
		PhysicsTimeMultiplier = 1.0f;
	}
}
