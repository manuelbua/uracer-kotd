package com.bitfire.uracer;

import java.lang.reflect.Field;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.utils.AMath;

public class URacer implements ApplicationListener {
	private Screen screen;
	private static boolean running = false;

	private static ScalingStrategy scalingStrategy;
	private float temporalAliasing = 0;
	private float timeAccumSecs = 0;
	private float oneOnOneBillion = 0;
	public static float timeMultiplier = 0f;
	private static boolean hasStepped = false;

	// stats
	private static float graphicsTime = 0;
	private static float physicsTime = 0;
	private static float aliasingTime = 0;
	private static final float MaxDeltaTime = 0.25f;
	private static long frameCount = 0;

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
		// extrapolate version information
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

		// computed for a 256px tile size target (compute needed conversion factors)
		scalingStrategy = new ScalingStrategy( new Vector2( 1280, 800 ), 70f, 224, 1f );

		Art.init( scalingStrategy.invTileMapZoomFactor );
		Sounds.init();

		Config.asDefault();
		Gdx.graphics.setVSync( true );

		running = true;
		oneOnOneBillion = 1.0f / 1000000000.0f;
		temporalAliasing = 0;
		timeMultiplier = Config.Physics.PhysicsTimeMultiplier;

		// ensures the first iteration ever is going to at least perform one single tick
		lastDeltaTimeSec = MaxDeltaTime;
		timeAccumSecs = Config.Physics.PhysicsDt;

		setScreen( new GameScreen() );
	}

	// private long lastTimeNs = 0;
	private static float lastDeltaTimeSec;

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
		// long currNanos = System.nanoTime();
		// lastDeltaTimeSec = (currNanos - lastTimeNs) * oneOnOneBillion;
		// lastTimeNs = currNanos;

		lastDeltaTimeSec = Gdx.graphics.getDeltaTime();

		// avoid spiral of death
		lastDeltaTimeSec = AMath.clamp( lastDeltaTimeSec, 0, MaxDeltaTime );

		long startTime = System.nanoTime();
		{
			hasStepped = false;
			timeAccumSecs += lastDeltaTimeSec * timeMultiplier;
			while( timeAccumSecs > Config.Physics.PhysicsDt ) {
				screen.tick();
				timeAccumSecs -= Config.Physics.PhysicsDt;
				hasStepped = true;
				if( screen.quit() ) {
					return;
				}
			}

			// simulate slowness
			// try { Thread.sleep( 32 ); } catch( InterruptedException e ) {}
		}

		physicsTime = (System.nanoTime() - startTime) * oneOnOneBillion;

		// compute the temporal aliasing factor, entities will render
		// themselves accordingly to this to avoid flickering and
		// permitting slow-motion effects without artifacts.
		// (this imply accepting a one-frame-behind behavior)
		temporalAliasing = timeAccumSecs * Config.Physics.PhysicsTimestepHz;
		aliasingTime = temporalAliasing;

		startTime = System.nanoTime();
		{
			screen.render();

			// simulate slowness
			// if(Config.isDesktop)
			// try { Thread.sleep( 5 ); } catch( InterruptedException e ) {}
		}

		graphicsTime = (System.nanoTime() - startTime) * oneOnOneBillion;
		frameCount++;
		// mean.addValue( graphicsTime );
		// if((frameCount&0x3f)==0) System.out.println("gfx-mean="+mean.getMean());
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
		screen.resume();
	}

	@Override
	public void dispose() {
		setScreen( null );

		Art.dispose();
		Sounds.dispose();

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
			screen.init();
		}
	}

	public static boolean isRunning() {
		return running;
	}

	public static boolean hasStepped() {
		return hasStepped;
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

	public static float getTemporalAliasing() {
		return aliasingTime;
	}

	public static long getFrameCount() {
		return frameCount;
	}

	public static String getVersionInfo() {
		return versionInfo;
	}

	public static ScalingStrategy getScalingStrategy() {
		return scalingStrategy;
	}
}
