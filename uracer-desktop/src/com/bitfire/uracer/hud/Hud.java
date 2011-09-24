package com.bitfire.uracer.hud;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.GameLogic;
import com.bitfire.uracer.simulations.car.Replay;

public class Hud
{
	private GameLogic logic;
	private SpriteBatch textBatch;
	private static Messager msg;

	private int gridX = 0;
	private String lapTimeFormat = "%.04fs";

	public Hud( GameLogic logic )
	{
		this.logic = logic;

		// setup sprite batch
		textBatch = new SpriteBatch( 1000, 10 );

		// y-flip
		Matrix4 proj = new Matrix4();
		proj.setToOrtho( 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0, 0, 10 );
		textBatch.setProjectionMatrix( proj );

		msg = new Messager();

		// lap time position
		gridX = (int)((float)Gdx.graphics.getWidth() / 4f);
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

	public void render()
	{
		textBatch.begin();

		msg.render( textBatch );

		// this lap time
		Art.fontCurse.drawMultiLine( textBatch, String.format( "YOUR TIME\n%.04fs", logic.getLapInfo().getElapsedSeconds() ), gridX, 10 );

		// best lap time
		Replay best = logic.getLapInfo().getBestReplay();
		Replay last = logic.getLapInfo().getLastReplay();

		if( best != null )
		{
			// has best
			Art.fontCurse.drawMultiLine( textBatch, String.format( "BEST TIME\n%.04fs", best.trackTimeSeconds ), gridX * 2, 10 );
		}
		else if( last != null && last.isValid )
		{
			// has only last
			Art.fontCurse.drawMultiLine( textBatch, String.format( "BEST TIME\n%.04fs", last.trackTimeSeconds ), gridX * 2, 10 );
		}
		else
		{
			// no data
			Art.fontCurse.drawMultiLine( textBatch, "BEST TIME\n- : ----", gridX * 2, 10 );
		}

		textBatch.end();
	}

	public static void showMessage( String message, float durationSecs )
	{
		msg.add( message, durationSecs );
	}
}
