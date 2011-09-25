package com.bitfire.uracer.hud;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.game.logic.GameLogic;
import com.bitfire.uracer.messager.Messager;
import com.bitfire.uracer.simulations.car.Replay;

public class Hud
{
	private GameLogic logic;
	private SpriteBatch textBatch;

	private static Messager msg;

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

	public void dispose()
	{
		textBatch.dispose();
	}

	public void tick()
	{
		Messager.tick();
	}

	public void render()
	{
		textBatch.begin();
		Messager.render( textBatch );
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
			Art.fontCurseYR.drawMultiLine( textBatch, String.format( "LAST TIME\n%.04fs", last.trackTimeSeconds ), gridX * 3, 10 );
		} else
		{
			// no data
			Art.fontCurseYR.drawMultiLine( textBatch, "\n- : ----", gridX * 3, 10 );
		}
	}
}
