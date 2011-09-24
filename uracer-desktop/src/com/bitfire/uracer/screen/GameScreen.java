package com.bitfire.uracer.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.GL20;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.debug.Debug;

public class GameScreen extends Screen
{
	public GameScreen()
	{
	}

	@Override
	public void tick()
	{
		if( Input.isOn( Keys.SPACE ) )
		{
			System.out.println( "JUMP" + jump++ );
		}
	}

	static int jump = 0;

	@Override
	public void render()
	{
		GL20 gl = Gdx.graphics.getGL20();

		gl.glClearColor( 0.15f, 0.15f, 0.15f, 1 );
		gl.glClear( GL20.GL_COLOR_BUFFER_BIT );

		// batch.draw( Art.titleScreen, 0, 0 );

		Debug.begin();

		int h = Gdx.graphics.getHeight();
		Debug.drawString( "FPS: " + Gdx.graphics.getFramesPerSecond(), 5, h - 30 );
		Debug.drawString( "dt: " + Gdx.graphics.getDeltaTime(), 5, h - 23 );

		Debug.drawString( "[touch] x = " + Input.getX(), 0, 135 );
		Debug.drawString( "[touch] y = " + Input.getY(), 0, 142 );
		Debug.drawString( "isTouching = " + Input.isTouching(), 0, 149 );
		Debug.drawString( "isDragging = " + Input.isDragging(), 0, 156 );

		Debug.drawString( "gdx = " + Gdx.input.isKeyPressed( Keys.SPACE ), 0, 128 );

		Debug.end();
	}
}
