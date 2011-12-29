package com.bitfire.uracer;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.bitfire.uracer.effects.TrackEffects;
import com.bitfire.uracer.game.logic.DirectorController.InterpolationMode;

public class Config
{
	// generic
	public static boolean isDesktop;

	public static class Graphics
	{
		public static boolean EnableMipMapping;
		public static boolean EnablePostProcessingFx;
		public static boolean SubframeInterpolation;
		public static InterpolationMode CameraInterpolationMode;
		public static long Effects;

		// wrap it out, ie. BitValue or else
		public static boolean hasEffect( long effectId )
		{
			return ((Effects & effectId) == effectId);
		}

		public static void asDefault()
		{
			EnablePostProcessingFx = false;
			EnableMipMapping = true;
			SubframeInterpolation = true;
			CameraInterpolationMode = InterpolationMode.Sigmoid;
			Effects = (TrackEffects.Effects.CarSkidMarks.id | TrackEffects.Effects.SmokeTrails.id);
		}
	}

	public static class Physics
	{

		public static float PixelsPerMeter; // defines how many pixels are 1 Box2d meter
		public static float PhysicsTimestepHz; // defines physics dt duration
		public static float PhysicsTimeMultiplier; // defines time modifier

		public static void asDefault()
		{
			PixelsPerMeter = 18.0f;
			PhysicsTimestepHz = 60.0f;
			PhysicsTimeMultiplier = 1f;
		}
	}

	public static class Debug
	{
		public static boolean dbgTraverseWalls;
		public static boolean dbgDirectorHasBounds;

		public static void asDefault()
		{
			dbgTraverseWalls = false;
			dbgDirectorHasBounds = true;
		}
	}

	// set default configuration values
	public static void asDefault()
	{
		isDesktop = (Gdx.app.getType() == ApplicationType.Desktop);

		Graphics.asDefault();
		Physics.asDefault();
		Debug.asDefault();
	}
}
