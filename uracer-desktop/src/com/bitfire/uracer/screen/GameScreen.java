package com.bitfire.uracer.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.bitfire.uracer.Input;

public class GameScreen extends Screen
{
	private BitmapFont font;

	public GameScreen()
	{
		font = new BitmapFont();
	}

	@Override
	public void removed()
	{
		super.removed();
		font.dispose();
	}

	@Override
	public void render(float timeAliasingFactor)
	{
		GL20 gl = Gdx.graphics.getGL20();

		gl.glClearColor( 0.15f, 0.15f, 0.15f, 1 );
		gl.glClear( GL20.GL_COLOR_BUFFER_BIT );

		spriteBatch.begin();
		font.draw( spriteBatch, "fps: " + Gdx.graphics.getFramesPerSecond() + ", delta: " + Gdx.graphics.getDeltaTime(), 10, 45 );
		font.draw( spriteBatch, "time aliasing alpha: " + timeAliasingFactor, 10, 25 );
		spriteBatch.end();
	}

	@Override
	public void tick( Input input )
	{
	}
}
