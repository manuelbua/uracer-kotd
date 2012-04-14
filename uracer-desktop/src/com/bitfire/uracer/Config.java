package com.bitfire.uracer;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.bitfire.uracer.game.logic.helpers.DirectorController.InterpolationMode;
import com.bitfire.uracer.postprocessing.filters.Blur;
import com.bitfire.uracer.postprocessing.filters.Blur.BlurType;
import com.bitfire.uracer.postprocessing.filters.ZoomBlur;

public final class Config {
	// generic
	public static boolean isDesktop;
	public static final String LevelsStore = "data/levels/";

	public static final class PostProcessing {
		public static BlurType BlurType;
		public static float RttRatio = 0.25f;
		public static int PotRttFboWidth, PotRttFboHeight;
		public static int RttFboWidth, RttFboHeight;

		public static ZoomBlur.Quality ZoomQuality;
		public static float ZoomMaxStrength;

		// compute per-resolution constants
		public static void asDefault() {
			int w = Gdx.graphics.getWidth();

			RttFboWidth = (int)(Gdx.graphics.getWidth() * RttRatio);
			RttFboHeight = (int)(Gdx.graphics.getHeight() * RttRatio);

			if( w >= 1680 ) {
				BlurType = Blur.BlurType.Gaussian5x5b;
				ZoomQuality = ZoomBlur.Quality.Normal;
				ZoomMaxStrength = -0.08f;
				PotRttFboWidth = 256;
				PotRttFboHeight = 256;
			} else if( w >= 1280 ) {
				BlurType = Blur.BlurType.Gaussian3x3b;
				ZoomQuality = ZoomBlur.Quality.Medium;
				ZoomMaxStrength = -0.08f;
				PotRttFboWidth = 256;
				PotRttFboHeight = 256;
			} else if( w >= 800 ) {
				BlurType = Blur.BlurType.Gaussian3x3;
				ZoomQuality = ZoomBlur.Quality.Low;
				ZoomMaxStrength = -0.08f;
				PotRttFboWidth = 128;
				PotRttFboHeight = 128;
			}

			Gdx.app.log( "Config", "blurType=" + BlurType );
			Gdx.app.log( "Config", "zoomQuality=" + ZoomQuality );
			Gdx.app.log( "Config", "zoomMaxStrength=" + ZoomMaxStrength );
			Gdx.app.log( "Config", "SmallFboWidth=" + PotRttFboWidth );
			Gdx.app.log( "Config", "SmallFboHeight=" + PotRttFboHeight );
			Gdx.app.log( "Config", "FBO x Ratio=" + (int)(Gdx.graphics.getWidth() * RttRatio) + "x" + (int)(Gdx.graphics.getHeight() * RttRatio) );
		}

		private PostProcessing() {
		}
	}

	public static final class Graphics {
		public static boolean EnableMipMapping;
		public static boolean EnablePostProcessingFx;
		public static boolean SubframeInterpolation;
		public static boolean RenderBox2DWorldWireframe;
		public static boolean RenderPlayerDebugInfo;
		public static boolean RenderHudDebugInfo;
		public static boolean Render3DBoundingBoxes;
		public static boolean DumbNightMode;	// night-mode rendered as an overlay *after* PostProcessor
		public static InterpolationMode CameraInterpolationMode;


		public static void asDefault() {
			EnablePostProcessingFx = true;
			EnableMipMapping = true;
			SubframeInterpolation = true;

			DumbNightMode = false;
			RenderBox2DWorldWireframe = false;
			RenderPlayerDebugInfo = true;
			RenderHudDebugInfo = true;
			Render3DBoundingBoxes = false;

			CameraInterpolationMode = InterpolationMode.Sigmoid;
		}

		private Graphics() {
		}
	}

	public static final class Physics {

		/** defines how many pixels are 1 Box2d meter, this will be
		 * automatically scaled accordingly to the device resolution
		 * during the construction of GameData */
		public static float PixelsPerMeter;

		/** defines physics dt duration, in hz */
		public static float PhysicsTimestepHz;

		/** defines time modifier */
		public static float PhysicsTimeMultiplier;

		/** defines physics dt duration, in seconds */
		public static float PhysicsDt;

		public static void asDefault() {
			PixelsPerMeter = 18.0f;
			PhysicsTimestepHz = 60.0f;
			PhysicsDt = 1.0f / PhysicsTimestepHz;
			PhysicsTimeMultiplier = 1f;
		}

		private Physics() {
		}
	}

	public static final class Debug {
		public static boolean TraverseWalls;
		public static boolean ApplyCarFrictionFromMap;
		public static boolean FrustumCulling;

		public static void asDefault() {
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

		Graphics.asDefault();
		Physics.asDefault();
		PostProcessing.asDefault();

		// always call as last
		Debug.asDefault();
	}

	private Config() {
	}
}
