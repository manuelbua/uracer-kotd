
package com.bitfire.uracer.configuration;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.bitfire.postprocessing.filters.Blur;
import com.bitfire.postprocessing.filters.Blur.BlurType;
import com.bitfire.uracer.game.logic.helpers.CameraController.InterpolationMode;
import com.bitfire.uracer.utils.NumberString;

public final class Config {
	public static boolean isDesktop;
	public static String UserPreferences = "uracer-preferences";

	public static final class Graphics {
		public static boolean EnableMipMapping;
		public static boolean SubframeInterpolation;
		public static InterpolationMode CameraInterpolationMode;

		public static void asDefault () {
			EnableMipMapping = true;
			SubframeInterpolation = true;
			CameraInterpolationMode = InterpolationMode.Linear;
		}

		private Graphics () {
		}
	}

	public static final class Physics {

		/** defines how many pixels are 1 Box2d meter */
		public static float PixelsPerMeter;

		/** defines physics dt duration, in hz */
		public static float PhysicsTimestepHz;

		/** defines the reference dt, in hz, to base damping and friction values on */
		public static float PhysicsTimestepReferenceHz;

		/** defines time modifier */
		public static float PhysicsTimeMultiplier;

		/** defines physics dt duration, in seconds */
		public static float PhysicsDt;

		public static void asDefault () {
			PixelsPerMeter = 18.0f;
			PhysicsTimestepHz = 60.0f;
			PhysicsTimestepReferenceHz = 60.0f;
			PhysicsDt = 1.0f / PhysicsTimestepHz;
			PhysicsTimeMultiplier = 1f;

			Gdx.app.log("Config", "Physics at " + PhysicsTimestepHz + "Hz (dt=" + NumberString.formatLong(PhysicsDt) + ")");
		}

		private Physics () {
		}
	}

	public static final class Debug {
		public static boolean UseDebugHelper;
		public static boolean RenderBox2DWorldWireframe;
		public static boolean RenderPlayerDebugInfo;
		public static boolean RenderHudDebugInfo;
		public static boolean Render3DBoundingBoxes;
		public static boolean RenderDebugInfoGraphics;
		public static boolean RenderDebugInfoFpsStats;
		public static boolean RenderDebugInfoMeshStats;
		public static boolean RenderDebugInfoMemoryStats;
		public static boolean RenderDebugInfoPostProcessor;
		public static boolean RenderDebugDrawsInTransitions;

		public static boolean TraverseWalls;
		public static boolean ApplyCarFrictionFromMap;
		public static boolean FrustumCulling;
		public static boolean InfiniteDilationTime;

		public static void asDefault () {

			UseDebugHelper = true;
			RenderBox2DWorldWireframe = false;
			RenderPlayerDebugInfo = false;
			RenderHudDebugInfo = Config.isDesktop;
			RenderDebugInfoFpsStats = true;
			RenderDebugInfoGraphics = true;
			RenderDebugInfoMemoryStats = Config.isDesktop;
			RenderDebugInfoMeshStats = Config.isDesktop;
			RenderDebugInfoPostProcessor = false;
			Render3DBoundingBoxes = false;
			RenderDebugDrawsInTransitions = true;

			TraverseWalls = false;
			ApplyCarFrictionFromMap = true;
			FrustumCulling = true;
			InfiniteDilationTime = false;
		}

		private Debug () {
		}
	}

	public static final class PostProcessing {

		public static BlurType BlurType;
		public static int ScaledFboWidth, ScaledFboHeight;
		private static float RttRatio = 0.25f;

		// compute per-resolution constants
		public static void asDefault () {

			ScaledFboWidth = (int)(Gdx.graphics.getWidth() * RttRatio);
			ScaledFboHeight = (int)(Gdx.graphics.getHeight() * RttRatio);

			int w = Gdx.graphics.getWidth();
			if (w >= 1400) {
				BlurType = Blur.BlurType.Gaussian5x5b;
			} else if (w >= 1200) {
				BlurType = Blur.BlurType.Gaussian5x5b;
			} else if (w >= 800) {
				BlurType = Blur.BlurType.Gaussian5x5b;
			}

			Gdx.app.log("Config", "blurType=" + BlurType);
		}

		private PostProcessing () {
		}
	}

	// set default configuration values
	public static void asDefault () {
		isDesktop = (Gdx.app.getType() == ApplicationType.Desktop);

		Debug.asDefault();
		Graphics.asDefault();
		Physics.asDefault();
		PostProcessing.asDefault();
	}

	private Config () {
	}
}
