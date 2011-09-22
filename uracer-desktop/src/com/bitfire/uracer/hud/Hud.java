package com.bitfire.uracer.hud;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Lap;

public class Hud
{
	private SpriteBatch textBatch;
	private Messager msg;
	private Lap lap;

	private int lapTimeX = 0;
	private String lapTimeFormat = "%.04fs";

	public Hud()
	{
		// setup sprite batch
		textBatch = new SpriteBatch( 1000, 10 );

		// y-flip
		Matrix4 proj = new Matrix4();
		proj.setToOrtho( 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0, 0, 10 );
		textBatch.setProjectionMatrix( proj );

		msg = new Messager();

		// lap time position
		lapTimeX = (int)( ((float)Gdx.graphics.getWidth() - Art.fontCurse.getBounds( String.format( lapTimeFormat, 9f ) ).width) / 2f );
	}

	public void dispose()
	{
		textBatch.dispose();
		msg.dispose();
	}

	public void reset()
	{
		msg.reset();
	}

	public void tick()
	{
		msg.update();
	}

	public void setLap(Lap lap)
	{
		this.lap = lap;
	}

	public void render()
	{
		textBatch.begin();

		msg.render( textBatch );
		renderLapTime();

		textBatch.end();
	}

	private void renderLapTime()
	{
		Art.fontCurse.draw( textBatch, "LAP TIME", lapTimeX - 5, 10 );
		Art.fontCurse.draw( textBatch, String.format( "%.04fs", lap.getElapsedSeconds() ), lapTimeX, 55 );
	}

	public void showMessage( String message, float durationSecs )
	{
		msg.add( message, durationSecs );
	}
}
