package com.bitfire.uracer.hud;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.effects.TrackEffects;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.game.logic.GameLogic;
import com.bitfire.uracer.messager.Messager;
import com.bitfire.uracer.simulations.car.Replay;

public class Hud
{
	private GameLogic logic;
	private SpriteBatch textBatch;
	private Car player;

	private HudLabel best, curr, last;
	private HudDebugMeter meterLatForce, meterSkidMarks;

	public Hud( GameLogic logic )
	{
		this.logic = logic;
		player = logic.getGame().getLevel().getPlayer();

		// setup sprite batch
		textBatch = new SpriteBatch( 100, 1 );

		// y-flip
		Matrix4 proj = new Matrix4();
		proj.setToOrtho( 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0, 0, 10 );
		textBatch.setProjectionMatrix( proj );

		// grid-based position
		int gridX = (int)((float)Gdx.graphics.getWidth() / 5f);

		best = new HudLabel( Art.fontCurseYR, "BEST  TIME\n-.----" );
		curr = new HudLabel( Art.fontCurseYR, "YOUR  TIME\n-.----" );
		last = new HudLabel( Art.fontCurseYR, "LAST  TIME\n-.----" );

		curr.setPosition( gridX - curr.getBounds().width, 10 );
		last.setPosition( gridX * 4 - last.getBounds().width, 10 );
		best.setPosition( gridX * 5 - best.getBounds().width, 10 );

		// meter lateral forces
		meterLatForce = new HudDebugMeter( this, 0, 100, 5 );
		float maxGrip = player.getCarModel().max_grip;
		meterLatForce.setLimits( 0, maxGrip );
		meterLatForce.setName( "lat-force-FRONT" );

		// meter skid marks count
		meterSkidMarks= new HudDebugMeter( this, 1, 100, 5 );
		meterSkidMarks.setLimits( 0, TrackEffects.MaxSkidMarks );
		meterSkidMarks.setName( "skid marks count" );
	}

	public GameLogic getLogic()
	{
		return logic;
	}

	public void dispose()
	{
		textBatch.dispose();
	}

	public void tick()
	{
		Messager.tick();
	}

	private void updateLapTimes()
	{
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
			} else
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

		updateLapTimes();
		curr.render( textBatch );
		best.render( textBatch );
		last.render( textBatch );

		if( Config.isDesktop )
		{
			meterLatForce.setValue( player.getSimulator().lateralForceFront.y );
			meterLatForce.render( textBatch );
			meterSkidMarks.setValue( TrackEffects.visibleDriftsCount );
			meterSkidMarks.render( textBatch );
		}

		textBatch.end();
	}

	public void debug()
	{
		meterLatForce.debug();
		meterSkidMarks.debug();
	}
}
