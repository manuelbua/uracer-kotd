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

	// tilemap zoom factor
	public static float TileMapZoomFactor;

	public static void asDefault( float tileMapZoomFactor )
	{
		TileMapZoomFactor = tileMapZoomFactor;
		SubframeInterpolation = true;
		PixelsPerMeter = 9.0f / TileMapZoomFactor;
		PhysicsTimestepHz = 60.0f;
		PhysicsTimeMultiplier = 1f;
	}
}
