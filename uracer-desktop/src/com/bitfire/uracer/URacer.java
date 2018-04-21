
package com.bitfire.uracer;

import java.lang.reflect.Field;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.TimeUtils;
import com.bitfire.uracer.configuration.BootConfig;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.configuration.Storage;
import com.bitfire.uracer.configuration.UserPreferences;
import com.bitfire.uracer.game.GameLevels;
import com.bitfire.uracer.game.logic.gametasks.hud.HudLabel;
import com.bitfire.uracer.game.logic.gametasks.hud.HudLabelAccessor;
import com.bitfire.uracer.game.logic.gametasks.messager.Message;
import com.bitfire.uracer.game.logic.gametasks.messager.MessageAccessor;
import com.bitfire.uracer.game.logic.types.helpers.TimeModulator;
import com.bitfire.uracer.game.screens.GameScreensFactory;
import com.bitfire.uracer.game.screens.GameScreensFactory.ScreenType;
import com.bitfire.uracer.game.screens.ScreensShared;
import com.bitfire.uracer.game.tween.SysTweener;
import com.bitfire.uracer.game.world.models.ModelFactory;
import com.bitfire.uracer.game.world.models.OrthographicAlignedStillModel;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.resources.BitmapFontFactory;
import com.bitfire.uracer.resources.Sounds;
import com.bitfire.uracer.screen.ScreenFactory;
import com.bitfire.uracer.screen.ScreenManager;
import com.bitfire.uracer.screen.TransitionFactory;
import com.bitfire.uracer.screen.TransitionFactory.TransitionType;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.BoxedFloat;
import com.bitfire.uracer.utils.BoxedFloatAccessor;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.ScaleUtils;
import com.bitfire.uracer.utils.SpriteBatchUtils;
import com.bitfire.uracer.utils.URacerRuntimeException;
import com.bitfire.utils.ShaderLoader;

import aurelienribon.tweenengine.Tween;

public class URacer implements ApplicationListener {
	public static final String Name = "URacer: The King Of The Drift";

	private final ScreenFactory screenFactory = new GameScreensFactory();
	private static ScreenManager screenMgr = null;
	private static Input input;
	private static boolean running = false;
	private static boolean resumed = false;
	private static boolean isDesktop = false;
	private static final boolean useRealFrametime = true;

	private float temporalAliasing = 0;
	private long timeAccuNs = 0;
	private long timeStepHz = 0;
	private long PhysicsDtNs = 0;
	private static long lastDeltaTimeNs = 0;
	private long lastDeltaTimeNsBeforePause = 0;
	private static float lastDeltaTimeSec = 0;
	private static float lastDeltaTimeMs = 0;

	private static final float oneOnOneBillion = 1.0f / 1000000000.0f;
	public static float timeMultiplier = 0f;

	private static float graphicsTime = 0;
	private static float physicsTime = 0;
	private static float aliasingTime = 0;
	public static final float MaxDeltaTimeSec = 0.25f;
	public static final long MaxDeltaTimeMs = (long)(MaxDeltaTimeSec * 1000f);
	public static final long MaxDeltaTimeNs = (long)(MaxDeltaTimeSec * 1000000000f);
	private static long frameCount = 0;
	private static long lastTicksCount = 0;

	// version
	public static final String versionInfo = getVersionInformation();
	private URacerFinalizer uRacerFinalizer;

	// boot
	private BootConfig boot;

	public interface URacerFinalizer {
		void dispose ();
	}

	public URacer (BootConfig boot) {
		running = true;
		this.boot = boot;

		Tween.registerAccessor(Message.class, new MessageAccessor());
		Tween.registerAccessor(HudLabel.class, new HudLabelAccessor());
		Tween.registerAccessor(BoxedFloat.class, new BoxedFloatAccessor());

		Convert.init(Config.Physics.PixelsPerMeter);

		// Initialize the timers after creating the game screen, so that there will be no huge discrepancies between the first
		// lastDeltaTimeSec value and the followers. Note those initial values are carefully choosen to ensure that the first
		// iteration ever is going to at least perform one single tick
		PhysicsDtNs = (long)((long)1000000000 / (long)Config.Physics.TimestepHz);
		timeStepHz = (long)Config.Physics.TimestepHz;
		timeAccuNs = PhysicsDtNs;

		temporalAliasing = 0;
		timeMultiplier = Config.Physics.TimeMultiplier;
		ShaderLoader.Pedantic = true;
	}

	public void setFinalizer (URacerFinalizer finalizer) {
		this.uRacerFinalizer = finalizer;
	}

	private static String getVersionInformation () {
		// extract version information
		String info = "";
		try {
			Field f = Class.forName("com.bitfire.uracer.VersionInfo").getDeclaredField("versionName");
			f.setAccessible(true);
			String value = f.get(null).toString();
			if (value.length() > 0) {
				info = value;
			}
		} catch (Exception e) {
			info = "(unversioned build)";
		}

		return info;
	}

	@Override
	public void create () {
		Gdx.app.setLogLevel(Application.LOG_DEBUG);
		isDesktop = (Gdx.app.getType() == ApplicationType.Desktop);

		System.out.println();
		Gdx.app.log("URacer", "Booting version " + URacer.versionInfo);
		Gdx.app.log("URacer", "GL vendor is " + Gdx.gl.glGetString(GL20.GL_VENDOR));
		Gdx.app.log("URacer", "GL version is " + Gdx.gl.glGetString(GL20.GL_VERSION));
		Gdx.app.log("URacer", "Java vendor is " + System.getProperty("java.vendor"));
		Gdx.app.log("URacer", "Java version is " + System.getProperty("java.version"));
		Gdx.app.log("URacer", "Using real frametime: " + (useRealFrametime ? "YES" : "NO"));
		Gdx.app.log("URacer", "Physics at " + timeStepHz + "Hz (dT=" + String.format("%.05f", Config.Physics.Dt) + ")");

		Storage.init();
		boot.store();
		UserPreferences.load();
		ScreensShared.loadFromUserPrefs();

		ScaleUtils.init(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		// create input system
		input = new Input(ScaleUtils.PlayViewport, 350, 50);
		Gdx.app.log("URacer", "Input system created.");

		Art.init();
		Sounds.init();
		TransitionFactory.init(screenFactory);
		SpriteBatchUtils.init(Art.debugFont, Art.DebugFontWidth);
		screenMgr = new ScreenManager(ScaleUtils.PlayViewport, screenFactory);

		// enumerate available game tracks
		try {
			GameLevels.init();
		} catch (URacerRuntimeException e) {
			Gdx.app.error("URacer", e.getMessage());
			System.exit(-1);
		}

		Game.show(ScreenType.MainScreen);
		// Game.show(ScreenType.GameScreen);
		// Screens.setScreen(ScreenType.OptionsScreen, TransitionType.CrossFader, 500);
	}

	@Override
	public void dispose () {
		UserPreferences.save();
		OrthographicAlignedStillModel.disposeShader();

		ModelFactory.dispose();
		screenMgr.dispose();
		TransitionFactory.dispose();
		BitmapFontFactory.dispose();

		Sounds.dispose();
		Art.dispose();
		SysTweener.dispose();

		if (uRacerFinalizer != null) {
			uRacerFinalizer.dispose();
		}

		System.exit(0);
	}

	// private void simulateSlowness (int millis) {
	// try {
	// Thread.sleep(millis);
	// } catch (InterruptedException e) {
	// }
	// }

	private long getDeltaTimeNs () {
		long delta = 0;

		if (!resumed) {
			if (useRealFrametime) {
				// this is not good for Android since the value often hop around
				delta = (long)(Gdx.graphics.getRawDeltaTime() * 1000000000f);
			} else {
				delta = (long)(Gdx.graphics.getDeltaTime() * 1000000000f);
			}
		} else {
			// if just resumed, then pick up the last delta time before pause
			delta = lastDeltaTimeNsBeforePause;
			resumed = false;
		}

		// avoid spiral of death
		return AMath.clamp(delta, 0, MaxDeltaTimeNs);
	}

	@Override
	public void render () {
		if (screenMgr.begin()) {

			lastDeltaTimeNs = getDeltaTimeNs();
			// Gdx.app.log("URacer", "lastdelta=" + lastDeltaTimeNs);

			// compute values in different units so that accessors will not
			// recompute them again and again
			lastDeltaTimeMs = (float)lastDeltaTimeNs / 1000000f;
			lastDeltaTimeSec = (float)lastDeltaTimeNs * oneOnOneBillion;
			// Gdx.app.log("URacer", "lastdelta_ms=" + lastDeltaTimeMs);

			// measure timings
			long startTime;

			/** tick */
			{
				lastTicksCount = 0;
				startTime = TimeUtils.nanoTime();
				timeAccuNs += lastDeltaTimeNs * timeMultiplier;
				while (timeAccuNs >= PhysicsDtNs) {
					lastTicksCount++;

					input.tick();
					screenMgr.tick();
					timeAccuNs -= PhysicsDtNs;
				}
				// simulateSlowness(48);
				physicsTime = (TimeUtils.nanoTime() - startTime) * oneOnOneBillion;
			}
			/** tick */

			/** tick completed */
			{
				// if the system has ticked, then trigger tickCompleted
				if (lastTicksCount > 0) {
					screenMgr.tickCompleted();
					if (screenMgr.quit()) {
						return;
					}
				}
			}
			/** tick completed */

			// compute the temporal aliasing factor, entities will render themselves accordingly to this to avoid flickering and
			// jittering, permitting slow-motion effects without artifacts (this imply accepting a one-frame-behind behavior)
			temporalAliasing = (timeAccuNs * timeStepHz) * oneOnOneBillion;
			aliasingTime = temporalAliasing;

			/** render */
			{
				startTime = TimeUtils.nanoTime();
				SysTweener.update();
				screenMgr.render();
				// simulateSlowness(30);
				graphicsTime = (TimeUtils.nanoTime() - startTime) * oneOnOneBillion;
			}
			/** render */

			frameCount++;
			screenMgr.end();
		}
	}

	@Override
	public void resize (int width, int height) {
		if (screenMgr != null) {
			screenMgr.resize(width, height);
		}
	}

	@Override
	public void pause () {
		if (Config.Debug.PauseDisabled) {
			Gdx.app.log("URacer", "Ignoring pause request by focus lost");
			return;
		}

		running = false;
		resumed = false;
		lastDeltaTimeNsBeforePause = lastDeltaTimeNs;
		screenMgr.pause();
	}

	@Override
	public void resume () {
		if (Config.Debug.PauseDisabled) {
			Gdx.app.log("URacer", "Ignoring resume request by focus gained");
			return;
		}

		if (!running) {
			resumed = true;
		}

		running = true;
		physicsTime = 0;
		graphicsTime = 0;
		screenMgr.resume();
	}

	//
	// export utilities
	//

	public static final class Game {
		public static boolean isDesktop () {
			return isDesktop;
		}

		public static boolean isRunning () {
			return running;
		}

		public static float getRenderTime () {
			return graphicsTime;
		}

		public static float getPhysicsTime () {
			return physicsTime;
		}

		public static float getLastDeltaSecs () {
			return lastDeltaTimeSec;
		}

		public static float getLastDeltaMs () {
			return lastDeltaTimeMs;
		}

		public static long getLastDeltaNs () {
			return lastDeltaTimeNs;
		}

		public static float getTemporalAliasing () {
			return aliasingTime;
		}

		public static long getFrameCount () {
			return frameCount;
		}

		public static long getLastTicksCount () {
			return lastTicksCount;
		}

		public static Input getInputSystem () {
			return input;
		}

		public static void resetTimeModFactor () {
			URacer.timeMultiplier = 1;
		}

		public static float getTimeModFactor () {
			return 1 - (URacer.timeMultiplier - TimeModulator.MinTime) / (TimeModulator.MaxTime - TimeModulator.MinTime);
		}

		public static void show (ScreenType screenType) {
			screenMgr.setScreen(screenType, TransitionType.Fader, 500);
		}

		public static void show (ScreenType screenType, int durationMs) {
			screenMgr.setScreen(screenType, TransitionType.Fader, durationMs);
		}

		public static void quit () {
			screenMgr.setScreen(ScreenType.ExitScreen, TransitionType.Fader, 500);
		}
	}

}
