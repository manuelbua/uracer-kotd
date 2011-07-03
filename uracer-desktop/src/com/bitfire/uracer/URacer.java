package com.bitfire.uracer;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL10;
import com.bitfire.uracer.screen.GameScreen;
import com.bitfire.uracer.screen.Screen;

public class URacer implements ApplicationListener
{
	private boolean running = false;
	private Screen screen;
	private Input input = new Input();
	private boolean started = false;


	private final float timestepHz = 60.0f;
	private final float oneOnTimestepHz = 1.0f / timestepHz;
	private float timeAccumSecs = 0;
	private float timeAliasingAlpha = 0;

	@Override
	public void create()
	{
		Gdx.input.setInputProcessor( input );
		running = true;
		setScreen( new GameScreen() );
	}

	@Override
	public void render()
	{
		Gdx.gl.glClear( GL10.GL_COLOR_BUFFER_BIT );
		timeAccumSecs += Gdx.graphics.getDeltaTime();
		while( timeAccumSecs > oneOnTimestepHz )
		{
			screen.tick( input );
			input.tick();
			timeAccumSecs -= oneOnTimestepHz;
		}

		timeAliasingAlpha = timeAccumSecs * timestepHz;	// opt away the divide-by-one-on-timestep
		screen.render();
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

}
