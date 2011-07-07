package com.bitfire.uracer.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Input;

public class TestScreen extends Screen
{
	private BitmapFont font;

	public TestScreen()
	{
		font = new BitmapFont( true );
	}

	@Override
	public void removed()
	{
		super.removed();
		font.dispose();
	}

	@Override
	public void tick( Input input )
	{
//		if(input.buttons[Input.JUMP] && !input.oldButtons[Input.JUMP])
		if(Input.isOn( Keys.SPACE ))
			System.out.println("JUMP"+ jump++);
	}

	static int jump = 0;

	@Override
	public void render(float timeAliasingFactor)
	{
		GL20 gl = Gdx.graphics.getGL20();

		gl.glClearColor( 0.15f, 0.15f, 0.15f, 1 );
		gl.glClear( GL20.GL_COLOR_BUFFER_BIT );

		spriteBatch.begin();

		spriteBatch.draw( Art.titleScreen, 0, 0 );

			int h = Gdx.graphics.getHeight();
			drawString( "FPS: " + Gdx.graphics.getFramesPerSecond(), 5, h-30 );
			drawString( "dt: " + Gdx.graphics.getDeltaTime(), 5, h-23 );
			drawString( "ta: " + timeAliasingFactor, 5, h-16 );

			drawString( "[touch] x = " + Input.getX(), 0, 135 );
			drawString( "[touch] y = " + Input.getY(), 0, 142 );
			drawString( "isTouching = " + Input.isTouching(), 0, 149 );
			drawString( "isDragging = " + Input.isDragging(), 0, 156 );

			drawString( "gdx = " + Gdx.input.isKeyPressed( Keys.SPACE ), 0, 128 );

		spriteBatch.end();
	}
}
