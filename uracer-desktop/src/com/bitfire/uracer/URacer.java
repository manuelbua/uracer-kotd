package com.bitfire.uracer;

import java.lang.reflect.Field;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.debug.Debug;
import com.bitfire.uracer.screen.GameScreen;
import com.bitfire.uracer.screen.Screen;
import com.bitfire.uracer.utils.AMath;

public class URacer implements ApplicationListener
{
	private Screen screen;
	private Input input = new Input();
	private boolean running = false;

	private float temporalAliasing = 0;
	private float timeAccumSecs = 0;
	private float oneOnOneBillion = 0;

	// stats
	private static float graphicsTime = 0;
	private static float physicsTime = 0;
	private static float aliasingTime = 0;

	// version
	private static String versionInfo;


	private static void updateVersionInformation()
	{
		// extrapolate version information
		versionInfo = "uRacer";
		try
		{
			Field f = Class.forName( "com.bitfire.uracer.VersionInfo" ).getDeclaredField( "versionName" );
			f.setAccessible( true );
			String value = f.get( null ).toString();
			if( value.length() > 0 )
			{
				versionInfo += " " + value;
			}
		}
		catch(Exception e)
		{}
	}

	@Override
	public void create()
	{
		URacer.updateVersionInformation();

		Config.asDefault();
		Art.load();
		Debug.create();
		input.releaseAllKeys();

		Physics.create( new Vector2( 0, 0 ), false );

		Gdx.input.setInputProcessor( input );
		Gdx.graphics.setVSync( true );

		running = true;
		oneOnOneBillion = 1.0f / 1000000000.0f;
		temporalAliasing = 0;

		setScreen( new GameScreen() );
	}

	@Override
	public void render()
	{
		float deltaTime = Gdx.graphics.getDeltaTime();

		// avoid spiral of death
		deltaTime = AMath.clamp( deltaTime, 0, Config.MaxDeltaTime );

		long startTime = System.nanoTime();
		{
			timeAccumSecs += deltaTime * Config.PhysicsTimeMultiplier;
			while( timeAccumSecs > Physics.dt )
			{
				input.tick();
				screen.tick();
				timeAccumSecs -= Physics.dt;
			}

			// simulate slowness
//			try { Thread.sleep( 32 ); } catch( InterruptedException e ) {}

		}
		physicsTime = (System.nanoTime() - startTime) * oneOnOneBillion;

		// compute the temporal aliasing factor, entities will render
		// themselves accordingly to this to avoid flickering and
		// permitting slow-motion effects without artifacts.
		// (this imply accepting a one-frame-behind behavior)
		temporalAliasing = timeAccumSecs * Config.PhysicsTimestepHz;
		aliasingTime = temporalAliasing;

		startTime = System.nanoTime();
		{
			screen.render();

			// simulate slowness
//			try { Thread.sleep( 32 ); } catch( InterruptedException e ) {}
		}
		graphicsTime = (System.nanoTime() - startTime) * oneOnOneBillion;
	}

	@Override
	public void resize( int width, int height )
	{
	}

	@Override
	public void pause()
	{
		running = false;
	}

	@Override
	public void resume()
	{
		running = true;
	}

	@Override
	public void dispose()
	{
		Debug.dispose();
		Art.dispose();
	}

	public void setScreen( Screen newScreen )
	{
		if( screen != null )
		{
			screen.removed();
		}

		screen = newScreen;

		if( screen != null )
		{
			screen.init( this );
		}
	}

	public boolean isRunning()
	{
		return running;
	}

	public static float getRenderTime()
	{
		return graphicsTime;
	}

	public static float getPhysicsTime()
	{
		return physicsTime;
	}

	public static float getTemporalAliasing()
	{
		return aliasingTime;
	}

	public static String getVersionInfo()
	{
		return versionInfo;
	}
}
