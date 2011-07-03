package com.bitfire.uracer;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class URacer implements ApplicationListener
{
	private BitmapFont font;
	private SpriteBatch batch;

	@Override
	public void create()
	{
		font = new BitmapFont();
		batch = new SpriteBatch();
	}

	@Override
	public void resume()
	{
	}

	@Override
	public void render()
	{
		GL20 gl = Gdx.graphics.getGL20();

		gl.glClearColor( 0.15f, 0.15f, 0.15f, 1 );
		gl.glClear( GL20.GL_COLOR_BUFFER_BIT );

		batch.begin();
		font.draw( batch, "This is uRacer: The King Of The Drift!", 10, 25 );
		batch.end();
	}

	@Override
	public void resize( int width, int height )
	{
	}

	@Override
	public void pause()
	{
	}

	@Override
	public void dispose()
	{
	}

}
