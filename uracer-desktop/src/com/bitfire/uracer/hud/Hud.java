package com.bitfire.uracer.hud;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.GameLogic;
import com.bitfire.uracer.hud.Messager.MessagePosition;
import com.bitfire.uracer.hud.Messager.MessageType;
import com.bitfire.uracer.simulations.car.Replay;

public class Hud
{
	private GameLogic logic;
	private SpriteBatch textBatch;

	private static Messager msg;
	private static Color colorBad, colorGood, defaultColor;

	private int gridX = 0;
	private int lapTimeX = 0;

	public Hud( GameLogic logic )
	{
		this.logic = logic;

		// setup sprite batch
		textBatch = new SpriteBatch( 1000, 10 );

		// y-flip
		Matrix4 proj = new Matrix4();
		proj.setToOrtho( 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0, 0, 10 );
		textBatch.setProjectionMatrix( proj );

		// grid-based position
		gridX = (int)((float)Gdx.graphics.getWidth() / 4f);
		lapTimeX = gridX - (int)Art.fontCurseYR.getMultiLineBounds( "YOUR TIME" ).width;
	}

	public static void init()
	{
		msg = new Messager();
		defaultColor = new Color( 1, 1, 1, 1 );
		colorBad = new Color( 0.85f, 0.15f, 0.15f, 1 );
		colorGood = new Color( 0.15f, 0.85f, 0.15f, 1 );
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

		renderLapTimes();

		textBatch.end();
	}

	private void renderLapTimes()
	{
		// render this lap time
		Art.fontCurseYR.drawMultiLine( textBatch, String.format( "YOUR TIME\n%.04fs", logic.getLapInfo().getElapsedSeconds() ),
				lapTimeX, 10 );

		// render best lap time
		Replay best = logic.getLapInfo().getBestReplay();
		Replay last = logic.getLapInfo().getLastReplay();

		if( best != null )
		{
			// has best
			Art.fontCurseYR.drawMultiLine( textBatch, String.format( "BEST TIME\n%.04fs", best.trackTimeSeconds ), gridX * 3, 10 );
		} else if( last != null && last.isValid )
		{
			// has only last
			Art.fontCurseYR.drawMultiLine( textBatch, String.format( "BEST TIME\n%.04fs", last.trackTimeSeconds ), gridX * 3, 10 );
		} else
		{
			// no data
			Art.fontCurseYR.drawMultiLine( textBatch, "BEST TIME\n- : ----", gridX * 3, 10 );
		}
	}

	// messages utilities

	public static void showMessage( String message, float durationSecs )
	{
		Hud.showMessage( message, durationSecs, MessageType.Information, MessagePosition.Bottom );
	}

	public static void showMessage( String message, float durationSecs, MessageType type, MessagePosition position )
	{
		msg.add( message, durationSecs, type, position );
	}

}
