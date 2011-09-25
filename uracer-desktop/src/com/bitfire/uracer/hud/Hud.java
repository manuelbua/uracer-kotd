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

	private int gridX = 0;
	private int lapTimeX = 0;

	private Label best, curr, last;

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
		gridX = (int)((float)Gdx.graphics.getWidth() / 5f);

		best = new Label( Art.fontCurseYR, "BEST  TIME\n-.----" );
		curr = new Label( Art.fontCurseYR, "YOUR  TIME\n-.----" );
		last = new Label( Art.fontCurseYR, "LAST  TIME\n-.----" );

		curr.setPosition( gridX - curr.getBounds().width, 10 );
		last.setPosition( gridX*4 - best.getBounds().width, 10 );
		best.setPosition( gridX*5 - best.getBounds().width, 10 );
	}

	public void dispose()
	{
		textBatch.dispose();
	}

	public void tick()
	{
		Messager.tick();

		//
		// update time labels
		//

		// current time
		curr.setString( String.format( "YOUR  TIME\n%.04fs", logic.getLapInfo().getElapsedSeconds() ) );

		// render best lap time
		Replay rbest = logic.getLapInfo().getBestReplay();

		// best time
		if( rbest != null && rbest.isValid )
		{
			// has best
			best.setString( String.format( "BEST  TIME\n%.04fs", rbest.trackTimeSeconds ) );
		} else
		{
			// temporarily use last track time
			if( logic.getLapInfo().hasLastTrackTimeSeconds() )
			{
				best.setString( String.format( "BEST  TIME\n%.04fs", logic.getLapInfo().getLastTrackTimeSeconds() ) );
			}
			else
				best.setString( "BEST TIME\n-.----" );
		}

		// last time
		if( logic.getLapInfo().hasLastTrackTimeSeconds() )
		{
			// has only last
			last.setString( String.format( "LAST  TIME\n%.04fs", logic.getLapInfo().getLastTrackTimeSeconds() ) );
		} else
		{
			last.setString( "LAST  TIME\n-:----" );
		}
	}

	public void render()
	{
		textBatch.begin();
		Messager.render( textBatch );
		curr.render( textBatch );
		best.render( textBatch );
		last.render( textBatch );
		textBatch.end();
	}

}
