package com.bitfire.uracer;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.bitfire.uracer.effects.TrackEffects;
import com.bitfire.uracer.game.GameDifficulty;
import com.bitfire.uracer.game.logic.DirectorController.InterpolationMode;
import com.bitfire.uracer.postprocessing.filters.Blur;
import com.bitfire.uracer.postprocessing.filters.Blur.BlurType;

public class Config {
	// generic
	public static boolean isDesktop;

	public static class PostProcessing {
		public static BlurType BlurType;
		public static float RttRatio = 0.25f;
		public static int SmallFboWidth, SmallFboHeight;

		public static int ZoomQuality;
		public static float ZoomMaxStrength;

		// compute per-resolution constants
		public static void asDefault() {
			int w = Gdx.graphics.getWidth();

			if( w >= 1680 ) {
				BlurType = Blur.BlurType.Gaussian5x5b;
				ZoomQuality = 16;
				ZoomMaxStrength = -0.08f;
				SmallFboWidth = SmallFboHeight = 512;
			}
			else if( w >= 1280 ) {
				BlurType = Blur.BlurType.Gaussian3x3b;
				ZoomQuality = 4;
				ZoomMaxStrength = -0.08f;
				SmallFboWidth = SmallFboHeight = 256;
			}
			else if( w >= 800 ) {
				BlurType = Blur.BlurType.Gaussian3x3;
				ZoomQuality = 2;
				ZoomMaxStrength = -0.08f;
				SmallFboWidth = SmallFboHeight = 256;
			}

			System.out.println( "blurType=" + BlurType );
			System.out.println( "zoomQuality=" + ZoomQuality );
			System.out.println( "zoomMaxStrength=" + ZoomMaxStrength );
			System.out.println( "SmallFboWidth=" + SmallFboWidth );
			System.out.println( "SmallFboHeight=" + SmallFboHeight );
			System.out.println( "FBO x Ratio=" + (int)(Gdx.graphics.getWidth()*RttRatio) + "x" + (int)(Gdx.graphics.getHeight()*RttRatio) );
		}
	}

	public static class Graphics {
		public static boolean EnableMipMapping;
		public static boolean EnablePostProcessingFx;
		public static boolean SubframeInterpolation;
		public static boolean RenderBox2DWorldWireframe;
		public static boolean RenderPlayerDebugInfo;
		public static boolean RenderHudDebugInfo;
		public static boolean Render3DBoundingBoxes;
		public static InterpolationMode CameraInterpolationMode;
		public static long Effects;

		// wrap it out, ie. BitValue or else
		public static boolean hasEffect( long effectId ) {
			return ((Effects & effectId) == effectId);
		}

		public static void asDefault() {
			EnablePostProcessingFx = true;
			EnableMipMapping = true;
			SubframeInterpolation = true;

			RenderBox2DWorldWireframe = false;
			RenderPlayerDebugInfo = true;
			RenderHudDebugInfo = true;
			Render3DBoundingBoxes = false;

			CameraInterpolationMode = InterpolationMode.Sigmoid;
			Effects = (TrackEffects.Effects.CarSkidMarks.id | TrackEffects.Effects.SmokeTrails.id);
		}
	}

	public static class Physics {

		public static float PixelsPerMeter; // defines how many pixels are 1 Box2d meter
		public static float PhysicsTimestepHz; // defines physics dt duration
		public static float PhysicsTimeMultiplier; // defines time modifier

		public static void asDefault() {
			PixelsPerMeter = 18.0f;
			PhysicsTimestepHz = 60.0f;
			PhysicsTimeMultiplier = 1f;
		}
	}

	public static class Game {
		public static GameDifficulty difficulty;

		public static void asDefault() {
			difficulty = GameDifficulty.Hard;
		}
	}

	public static class Debug {
		public static boolean TraverseWalls;
		public static boolean DirectorHasBounds;
		public static boolean ApplyFrictionMap;
		public static boolean FrustumCulling;

		public static void asDefault() {
			TraverseWalls = false;
			DirectorHasBounds = ((Graphics.CameraInterpolationMode == InterpolationMode.Linear) || (Graphics.CameraInterpolationMode == InterpolationMode.Off));
			ApplyFrictionMap = true;
			FrustumCulling = true;
		}
	}

	// set default configuration values
	public static void asDefault() {
		isDesktop = (Gdx.app.getType() == ApplicationType.Desktop);

		Game.asDefault();
		Graphics.asDefault();
		Physics.asDefault();
		PostProcessing.asDefault();

		// always call as last
		Debug.asDefault();
	}
}
