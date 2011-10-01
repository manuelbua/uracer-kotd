package com.bitfire.uracer;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;

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

	// graphics
	public static boolean EnableMipMapping;

	// platform
	public static boolean isDesktop;

	// definitions
	public static float MaxDeltaTime;

	// debug
	public static boolean dbgTraverseWalls;
	public static boolean dbgDirectorHasBounds;

	// set default configuration values
	public static void asDefault()
	{
		SubframeInterpolation = true;
		PixelsPerMeter = 18.0f;
		PhysicsTimestepHz = 60.0f;
		PhysicsTimeMultiplier = 1f;
		MaxDeltaTime = 0.25f;	// 4fps
		EnablePostProcessingFx = false;
		EnableMipMapping = true;
		isDesktop = (Gdx.app.getType() == ApplicationType.Desktop);

		// debug
		dbgTraverseWalls = false;
		dbgDirectorHasBounds = true;
	}
}
