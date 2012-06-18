package com.bitfire.uracer.configuration;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.bitfire.uracer.game.logic.helpers.CameraController.InterpolationMode;
import com.bitfire.uracer.postprocessing.filters.Blur;
import com.bitfire.uracer.postprocessing.filters.Blur.BlurType;
import com.bitfire.uracer.postprocessing.filters.RadialBlur;
import com.bitfire.uracer.utils.NumberString;

public final class Config {
	// generic
	public static boolean isDesktop;
	public static final String LevelsStore = "data/levels/";
	public static final String ShapesStore = "data/base/";
	public static final String ShapesRefs = "../../data-src/base/cars/";
	public static final String URacerConfigFolder = "uracer/";
	public static final String LocalReplaysStore = "local-replays/";

	public static final class PostProcessing {
		public static boolean Enabled;
		public static boolean EnableVignetting;
		public static boolean EnableBloom;
		public static boolean EnableZoom;
		public static boolean EnableZoomRadialBlur;
		public static boolean EnableCrtScreen;
		public static boolean EnableRadialDistortion;	// if both this and the CrtScreen are enabled, then the crt shader will do the distortion instead.

		public static BlurType BlurType;
		public static float RttRatio = 0.25f;
		public static int RttFboWidth, RttFboHeight;

		public static RadialBlur.Quality RadialBlurQuality;
		public static float ZoomMaxStrength;

		// compute per-resolution constants
		public static void asDefault() {

			// post-processor
			Enabled = true;//Config.isDesktop;

			// post-processor effects
			EnableVignetting = true;//Config.isDesktop;
			EnableBloom = true;//Config.isDesktop;
			EnableZoom = true;
			EnableZoomRadialBlur = true;//Config.isDesktop;
			EnableCrtScreen = false;//Config.isDesktop;
			EnableRadialDistortion = false;

			RttFboWidth = (int)(Gdx.graphics.getWidth() * RttRatio);
			RttFboHeight = (int)(Gdx.graphics.getHeight() * RttRatio);

			int w = Gdx.graphics.getWidth();
			if( w >= 1680 ) {
				BlurType = Blur.BlurType.Gaussian5x5b;
				RadialBlurQuality = RadialBlur.Quality.High;
				ZoomMaxStrength = -0.08f;
			} else if( w >= 1280 ) {
				BlurType = Blur.BlurType.Gaussian3x3b;
				RadialBlurQuality = RadialBlur.Quality.Medium;
				ZoomMaxStrength = -0.08f;
			} else if( w >= 800 ) {
				BlurType = Blur.BlurType.Gaussian3x3;
				RadialBlurQuality = RadialBlur.Quality.Low;
				ZoomMaxStrength = -0.08f;
			}

			Gdx.app.log( "Config", "blurType=" + BlurType );
			Gdx.app.log( "Config", "zoomQuality=" + RadialBlurQuality );
			Gdx.app.log( "Config", "zoomMaxStrength=" + ZoomMaxStrength );
			Gdx.app.log( "Config", "FBO size is =" + (int)(Gdx.graphics.getWidth() * RttRatio) + "x" + (int)(Gdx.graphics.getHeight() * RttRatio) );
		}

		private PostProcessing() {
		}
	}

	public static final class Graphics {
		public static boolean EnableMipMapping;
		public static boolean SubframeInterpolation;
		public static boolean NightAsOverlay;	// night-mode rendered as an overlay *after* PostProcessor
		public static InterpolationMode CameraInterpolationMode;

		public static void asDefault() {
			EnableMipMapping = true;
			SubframeInterpolation = true;
			NightAsOverlay = false;
			CameraInterpolationMode = InterpolationMode.Sigmoid;
		}

		private Graphics() {
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

		public static void asDefault() {
			PixelsPerMeter = 18.0f;
			PhysicsTimestepHz = 60.0f;
			PhysicsTimestepReferenceHz = 60.0f;
			PhysicsDt = 1.0f / PhysicsTimestepHz;
			PhysicsTimeMultiplier = 1f;

			Gdx.app.log( "Config", "Physics at " + PhysicsTimestepHz + "Hz (dt=" + NumberString.formatLong( PhysicsDt ) + ")" );
		}

		private Physics() {
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

		public static boolean TraverseWalls;
		public static boolean ApplyCarFrictionFromMap;
		public static boolean FrustumCulling;

		public static void asDefault() {

			UseDebugHelper = true;
			RenderBox2DWorldWireframe = false;
			RenderPlayerDebugInfo = Config.isDesktop;
			RenderHudDebugInfo = Config.isDesktop;
			RenderDebugInfoFpsStats = true;
			RenderDebugInfoGraphics = true;
			RenderDebugInfoMemoryStats = Config.isDesktop;
			RenderDebugInfoMeshStats = Config.isDesktop;
			RenderDebugInfoPostProcessor = Config.isDesktop;
			Render3DBoundingBoxes = false;

			TraverseWalls = false;
			ApplyCarFrictionFromMap = true;
			FrustumCulling = true;
		}

		private Debug() {
		}
	}

	// set default configuration values
	public static void asDefault() {
		isDesktop = (Gdx.app.getType() == ApplicationType.Desktop);

		Debug.asDefault();
		Graphics.asDefault();
		Physics.asDefault();
		PostProcessing.asDefault();
	}

	private Config() {
	}
}
