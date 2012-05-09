package com.bitfire.uracer;

import java.lang.reflect.Field;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.resources.Sounds;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.SpriteBatchUtils;

public class URacer implements ApplicationListener {
	private Screen screen;
	private static boolean running = false;
	private static final boolean useRealFrametime = true;

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
	private static final float MaxDeltaTimeSec = 0.25f;
	private static final long MaxDeltaTimeNs = (long)(MaxDeltaTimeSec * 1000000000f);
	private static long frameCount = 0;
	private static long lastTicksCount = 0;

	// version
	private static String versionInfo;
	private URacerFinalizer uRacerFinalizer;

	public interface URacerFinalizer {
		void dispose();
	}

	public void setFinalizer( URacerFinalizer finalizer ) {
		this.uRacerFinalizer = finalizer;
	}

	private static void updateVersionInformation() {
		// extract version information
		versionInfo = "uRacer";
		try {
			Field f = Class.forName( "com.bitfire.uracer.VersionInfo" ).getDeclaredField( "versionName" );
			f.setAccessible( true );
			String value = f.get( null ).toString();
			if( value.length() > 0 ) {
				versionInfo += " " + value;
			}
		} catch( Exception e ) {
		}
	}

	@Override
	public void create() {
		URacer.updateVersionInformation();
		Gdx.app.log( "URacer", "booting version " + versionInfo );
		Gdx.app.log( "URacer", "Using real frametime: " + (useRealFrametime ? "YES" : "NO") );

		// computed for a 256px tile size target (compute needed conversion factors)
		scalingStrategy = new ScalingStrategy( new Vector2( 1280, 800 ), 70f, 224, 1f );

		// everything has been setup on a 256px tile, scale back if that's the case
		Config.asDefault();
		Config.Physics.PixelsPerMeter /= (scalingStrategy.targetScreenRatio / scalingStrategy.to256);

		Convert.init( scalingStrategy.invTileMapZoomFactor, Config.Physics.PixelsPerMeter );
		Art.init( scalingStrategy.invTileMapZoomFactor );
		SpriteBatchUtils.init( Art.debugFont, Art.DebugFontWidth );
		Sounds.init();

		Gdx.graphics.setVSync( true );

		running = true;
		temporalAliasing = 0;
		timeMultiplier = Config.Physics.PhysicsTimeMultiplier;

		PhysicsDtNs = (long)((long)1000000000 / (long)Config.Physics.PhysicsTimestepHz);
		timeStepHz = (long)Config.Physics.PhysicsTimestepHz;

		setScreen( new GameScreen() );

		// Initialize the timers after creating the game screen, so that there will be no huge discrepancies
		// between the first lastDeltaTimeSec value and the followers.
		// Note those initial values are carefully choosen to ensure that the first iteration ever is going to
		// at least perform one single tick
		lastTimeNs = TimeUtils.nanoTime();
		timeAccuNs = PhysicsDtNs;
	}

	// private WindowedMean mean = new WindowedMean( 120 );
	// NOTE: this render() method will get called by JoglGraphics when screen.tick will ask to finish!!
	@Override
	public void render() {
		if( screen == null ) {
			return;
		}

		if( screen.quit() ) {
			return;
		}

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
				screen.tick();
				timeAccuNs -= PhysicsDtNs;
				lastTicksCount++;
			}

			// simulate slowness
			// try { Thread.sleep( 32 ); } catch( InterruptedException e ) {}
		}

		physicsTime = (TimeUtils.nanoTime() - startTime) * oneOnOneBillion;

		// compute the temporal aliasing factor, entities will render
		// themselves accordingly to this to avoid flickering and jittering,
		// permitting slow-motion effects without artifacts.
		// (this imply accepting a max-one-frame-behind behavior)
		temporalAliasing = (timeAccuNs * timeStepHz) * oneOnOneBillion;
		aliasingTime = temporalAliasing;

		if( lastTicksCount > 0 )
		{
			screen.tickCompleted();

			if( screen.quit() ) {
				return;
			}
		}

		startTime = TimeUtils.nanoTime();
		{
			screen.render();

			// simulate slowness
			// if(Config.isDesktop)
			// try { Thread.sleep( 32 ); } catch( InterruptedException e ) {}
		}

		graphicsTime = (TimeUtils.nanoTime() - startTime) * oneOnOneBillion;
		frameCount++;

		screen.debugUpdate();
	}

	@Override
	public void resize( int width, int height ) {
	}

	@Override
	public void pause() {
		running = false;
		screen.pause();
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

		screen.resume();
	}

	@Override
	public void dispose() {
		setScreen( null );

		Sounds.dispose();
		Art.dispose();

		if( uRacerFinalizer != null ) {
			uRacerFinalizer.dispose();
		}

		// if(!Config.isDesktop || )
		System.exit( 0 );
	}

	public void setScreen( Screen newScreen ) {
		if( screen != null ) {
			screen.removed();
		}

		screen = newScreen;

		if( screen != null ) {
			screen.init( scalingStrategy );
		}
	}

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

	public static String getVersionInfo() {
		return versionInfo;
	}
}
