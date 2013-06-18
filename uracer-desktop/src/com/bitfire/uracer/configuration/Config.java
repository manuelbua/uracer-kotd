
package com.bitfire.uracer.configuration;

import com.bitfire.postprocessing.filters.Blur;
import com.bitfire.postprocessing.filters.Blur.BlurType;
import com.bitfire.uracer.game.logic.helpers.CameraController.InterpolationMode;

public final class Config {
	public static final class Graphics {
		public static final int ReferenceScreenWidth = 1280;
		public static final int ReferenceScreenHeight = 720;

		public static final int DefaultFadeMilliseconds = 500;
		public static final int DefaultResetFadeMilliseconds = 500;

		public static final boolean EnableMipMapping = true;
		public static final boolean SubframeInterpolation = true;
		public static final InterpolationMode CameraInterpolationMode = InterpolationMode.Linear;

		private Graphics () {
		}
	}

	public static final class Physics {

		/** defines how many pixels are 1 Box2d meter */
		public static final float PixelsPerMeter = 18.0f;

		/** defines physics dt duration, in hz */
		public static final float TimestepHz = 60.0f;

		/** defines the reference dt, in hz, to base damping and friction values on */
		public static final float PhysicsTimestepReferenceHz = 60.0f;;

		/** defines time modifier */
		public static final float TimeMultiplier = 1f;

		/** defines physics dt duration, in seconds */
		public static final float Dt = 1.0f / TimestepHz;

		/** defines the minimum and maximum collision impact force to be considered */
		public static final float MaxImpactForce = 200f;
		public static final float OneOnMaxImpactForce = 1f / MaxImpactForce;

		private Physics () {
		}
	}

	public static final class Debug {
		public static final boolean UseDebugHelper = true;
		public static boolean RenderBox2DWorldWireframe = false;
		public static final boolean RenderPlayerDebugInfo = false;
		public static final boolean RenderHudDebugInfo = true;
		public static boolean Render3DBoundingBoxes = false;
		public static final boolean RenderDebugInfoGraphics = true;
		public static final boolean RenderDebugInfoFpsStats = true;
		public static final boolean RenderDebugInfoMeshStats = true;
		public static final boolean RenderDebugInfoMemoryStats = false;
		public static final boolean RenderDebugInfoPostProcessor = true;
		public static final boolean RenderDebugDrawsInTransitions = true;
		public static boolean RenderTrackSectors = false;

		public static final boolean TraverseWalls = false;
		public static final boolean ApplyCarFrictionFromMap = true;
		public static final boolean FrustumCulling = true;
		public static final boolean InfiniteDilationTime = true;

		public static final boolean ShowAdvancedDebugInfo = true;

		private Debug () {
		}
	}

	public static final class PostProcessing {

		public static final BlurType BlurType = Blur.BlurType.Gaussian5x5b;
		public static final int BlurNumPasses = 2;
		public static final float NormalDepthMapRatio = 1f;
		public static final float FboRatio = 0.5f;

		private PostProcessing () {
		}
	}

	private Config () {
	}
}
