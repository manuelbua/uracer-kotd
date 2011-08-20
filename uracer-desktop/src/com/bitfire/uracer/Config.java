package com.bitfire.uracer;

public class Config
{
	// whether or not subframe interpolable entities should
	// perform interpolation
	public static boolean SubframeInterpolation;

	// defines how many pixels are 1 Box2d meter
	public static float PixelsPerMeter;

	// defines physics dt duration
	public static float PhysicsTimestepHz;

	// defines time modifier
	public static float PhysicsTimeMultiplier;

	// post-processing fx
	public static boolean EnablePostProcessingFx;

	// set default configuration values
	public static void asDefault()
	{
		SubframeInterpolation = true;
		PixelsPerMeter = 18.0f;
		PhysicsTimestepHz = 60.0f;
		PhysicsTimeMultiplier = 1f;
		EnablePostProcessingFx = false;
	}
}
