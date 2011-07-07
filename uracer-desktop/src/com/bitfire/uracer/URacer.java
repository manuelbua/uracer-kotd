package com.bitfire.uracer;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL10;
import com.bitfire.uracer.screen.GameScreen;
import com.bitfire.uracer.screen.Screen;

public class URacer implements ApplicationListener
{
	private Screen screen;
	private Input input = new Input();
	private boolean running = false;

	private final float timestepHz = 60.0f;
	private final float oneOnTimestepHz = 1.0f / timestepHz;
	private float timeAccumSecs = 0;
	private float timeAliasingAlpha = 0;

	@Override
	public void create()
	{
		Art.load();
		Gdx.graphics.setVSync( true );
		input.releaseAllKeys();
		Gdx.input.setInputProcessor( input );
		setScreen( new GameScreen() );

		running = true;
	}

	@Override
	public void render()
	{
		Gdx.gl.glClear( GL10.GL_COLOR_BUFFER_BIT );

		float deltaTime = Gdx.graphics.getDeltaTime();

		// avoid spiral of death
		if( deltaTime > 0.25f )
			deltaTime = 0.25f;

		timeAccumSecs += deltaTime;
		while( timeAccumSecs > oneOnTimestepHz )
		{
			input.tick();
			screen.tick( input );

			timeAccumSecs -= oneOnTimestepHz;
		}

		timeAliasingAlpha = timeAccumSecs * timestepHz; // opt away the divide-by-one-on-timestep
		screen.render( timeAliasingAlpha );
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
	}

	public void setScreen( Screen newScreen )
	{
		if( screen != null )
			screen.removed();

		screen = newScreen;

		if( screen != null )
			screen.init( this );
	}

	public boolean isRunning()
	{
		return running;
	}
}
