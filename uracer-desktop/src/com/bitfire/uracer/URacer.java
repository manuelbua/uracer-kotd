package com.bitfire.uracer;

import java.lang.reflect.Field;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.configuration.UserPreferences;
import com.bitfire.uracer.game.tween.SysTweener;
import com.bitfire.uracer.game.world.models.ModelFactory;
import com.bitfire.uracer.game.world.models.OrthographicAlignedStillModel;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.resources.Sounds;
import com.bitfire.uracer.screen.ScreenFactory;
import com.bitfire.uracer.screen.ScreenFactory.ScreenType;
import com.bitfire.uracer.screen.ScreenManager;
import com.bitfire.uracer.screen.TransitionFactory;
import com.bitfire.uracer.screen.TransitionFactory.TransitionType;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.SpriteBatchUtils;

public class URacer implements ApplicationListener {
	public static final String Name = "URacer: The King Of The Drift";

	private static ScreenManager screenMgr;
	private static Input input;
	private static boolean running = false;
	private static final boolean useRealFrametime = true;// Config.isDesktop;

	private static ScalingStrategy scalingStrategy;
	private float temporalAliasing = 0;
	private long timeAccuNs = 0;
	private long timeStepHz = 0;
	private long PhysicsDtNs = 0;
	private long lastTimeNs = 0;
	private static long lastDeltaTimeNs = 0;
	private static float lastDeltaTimeSec = 0;
	private static float lastDeltaTimeMs = 0;

	private static final float oneOnOneBillion = 1.0f / 1000000000.0f;
	public static float timeMultiplier = 0f;

	// stats
	private static float graphicsTime = 0;
	private static float physicsTime = 0;
	private static float aliasingTime = 0;
	public static final float MaxDeltaTimeSec = 0.25f;
	public static final long MaxDeltaTimeMs = (long)(MaxDeltaTimeSec * 1000f);
	public static final long MaxDeltaTimeNs = (long)(MaxDeltaTimeSec * 1000000000f);
	private static long frameCount = 0;
	private static long lastTicksCount = 0;

	// version
	private static String versionInfo = "";
	private URacerFinalizer uRacerFinalizer;

	public interface URacerFinalizer {
		void dispose();
	}

	public void setFinalizer( URacerFinalizer finalizer ) {
		this.uRacerFinalizer = finalizer;
	}

	public static String getVersionInformation() {
		if( versionInfo.length() == 0 ) {
			// extract version information
			String info = "";
			try {
				Field f = Class.forName( "com.bitfire.uracer.VersionInfo" ).getDeclaredField( "versionName" );
				f.setAccessible( true );
				String value = f.get( null ).toString();
				if( value.length() > 0 ) {
					info = value;
				}
			} catch( Exception e ) {
				info = "(version not found)";
			}

			versionInfo = info;
			return info;
		} else {
			return versionInfo;
		}
	}

	@Override
	public void create() {
		Gdx.app.setLogLevel( Application.LOG_DEBUG );
		versionInfo = URacer.getVersionInformation();

		Gdx.app.log( "URacer", "booting version " + versionInfo );
		Gdx.app.log( "URacer", "Using real frametime: " + (useRealFrametime ? "YES" : "NO") );

		// create input system
		input = new Input();
		Gdx.app.log( "URacer", "input system created." );

		// computed for a 256px tile size target (compute needed conversion
		// factors)
		scalingStrategy = new ScalingStrategy( new Vector2( 1280, 800 ), 70f, 224, 1f );

		ScreenFactory.init( scalingStrategy );

		// load default private configuration
		Config.asDefault();

		UserPreferences.load();

		Convert.init( scalingStrategy.tileMapZoomFactor, Config.Physics.PixelsPerMeter );
		Art.init( scalingStrategy.invTileMapZoomFactor );
		SpriteBatchUtils.init( Art.debugFont, Art.DebugFontWidth );
		Sounds.init();

		ModelFactory.init( scalingStrategy );

		Gdx.graphics.setVSync( true );

		running = true;
		temporalAliasing = 0;
		timeMultiplier = Config.Physics.PhysicsTimeMultiplier;

		PhysicsDtNs = (long)((long)1000000000 / (long)Config.Physics.PhysicsTimestepHz);
		timeStepHz = (long)Config.Physics.PhysicsTimestepHz;

		screenMgr = new ScreenManager( scalingStrategy );

		// screenMgr.setScreen( ScreenType.GameScreen, TransitionType.Fader, 250
		// );
		screenMgr.setScreen( ScreenType.MainScreen, TransitionType.Fader, 250 );

		// Initialize the timers after creating the game screen, so that there
		// will be no huge discrepancies
		// between the first lastDeltaTimeSec value and the followers.
		// Note those initial values are carefully choosen to ensure that the
		// first iteration ever is going to
		// at least perform one single tick
		lastTimeNs = TimeUtils.nanoTime();
		timeAccuNs = PhysicsDtNs;
		// try { Thread.sleep( 1000 ); } catch( InterruptedException e ) {}
	}

	@Override
	public void dispose() {
		UserPreferences.save();
		OrthographicAlignedStillModel.disposeShader();

		ModelFactory.dispose();
		screenMgr.dispose();
		TransitionFactory.dispose();

		Sounds.dispose();
		Art.dispose();
		SysTweener.dispose();

		if( uRacerFinalizer != null ) {
			uRacerFinalizer.dispose();
		}

		System.exit( 0 );
	}

	@Override
	public void render() {
		if( screenMgr.begin() ) {

			// this is not good for Android since the value often hop around
			if( useRealFrametime ) {
				long currTimeNs = TimeUtils.nanoTime();
				lastDeltaTimeNs = (currTimeNs - lastTimeNs);
				lastTimeNs = currTimeNs;
			} else {
				lastDeltaTimeNs = (long)(Gdx.graphics.getDeltaTime() * 1000000000);
			}

			// avoid spiral of death
			lastDeltaTimeNs = AMath.clamp( lastDeltaTimeNs, 0, MaxDeltaTimeNs );

			// compute values in different units so that accessors will not
			// recompute them again and again
			lastDeltaTimeMs = (float)(lastDeltaTimeNs / 1000000);
			lastDeltaTimeSec = (float)lastDeltaTimeNs * oneOnOneBillion;

			lastTicksCount = 0;
			long startTime = TimeUtils.nanoTime();
			{
				timeAccuNs += lastDeltaTimeNs * timeMultiplier;
				while( timeAccuNs > PhysicsDtNs ) {
					input.tick();
					screenMgr.tick();
					timeAccuNs -= PhysicsDtNs;
					lastTicksCount++;
				}

				// simulate slowness
				// try { Thread.sleep( 32 ); } catch( InterruptedException e )
				// {}
			}

			physicsTime = (TimeUtils.nanoTime() - startTime) * oneOnOneBillion;

			// if the system has ticked, then trigger tickCompleted
			if( lastTicksCount > 0 ) {
				screenMgr.tickCompleted();

				if( screenMgr.quit() ) {
					return;
				}
			}

			// compute the temporal aliasing factor, entities will render
			// themselves accordingly to this to avoid flickering and jittering,
			// permitting slow-motion effects without artifacts.
			// (this imply accepting a max-one-frame-behind behavior)
			temporalAliasing = (timeAccuNs * timeStepHz) * oneOnOneBillion;
			aliasingTime = temporalAliasing;

			startTime = TimeUtils.nanoTime();
			{
				SysTweener.update();
				screenMgr.render( null );

				// simulate slowness
				// try { Thread.sleep( 32 ); } catch( InterruptedException e )
				// {}
			}

			graphicsTime = (TimeUtils.nanoTime() - startTime) * oneOnOneBillion;
			frameCount++;

			screenMgr.debugRender();
			screenMgr.end();
		}
	}

	@Override
	public void resize( int width, int height ) {
	}

	@Override
	public void pause() {
		running = false;
		screenMgr.pause();
	}

	@Override
	public void resume() {
		running = true;
		lastTimeNs = TimeUtils.nanoTime();
		lastDeltaTimeNs = 0;
		lastDeltaTimeMs = 0;
		lastDeltaTimeSec = 0;

		physicsTime = 0;
		graphicsTime = 0;

		screenMgr.resume();
	}

	//
	// export utilities
	//

	public static final class Game {
		public static boolean isRunning() {
			return running;
		}

		public static float getRenderTime() {
			return graphicsTime;
		}

		public static float getPhysicsTime() {
			return physicsTime;
		}

		public static float getLastDeltaSecs() {
			return lastDeltaTimeSec;
		}

		public static float getLastDeltaMs() {
			return lastDeltaTimeMs;
		}

		public static float getTemporalAliasing() {
			return aliasingTime;
		}

		public static long getFrameCount() {
			return frameCount;
		}

		public static long getLastTicksCount() {
			return lastTicksCount;
		}

		public static Input getInputSystem() {
			return input;
		}

		public static void show( ScreenType screenType ) {
			Screens.setScreen( screenType, TransitionType.Fader, 250 );
		}

		public static void quit() {
			show( ScreenType.ExitScreen );
		}
	}

	public static final class Screens {
		public static void setScreen( ScreenType screenType, TransitionType transitionType, long transitionDurationMs ) {
			screenMgr.setScreen( screenType, transitionType, transitionDurationMs );
		}
	}

}
