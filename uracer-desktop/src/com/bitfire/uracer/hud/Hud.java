package com.bitfire.uracer.hud;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.effects.CarSkidMarks;
import com.bitfire.uracer.effects.TrackEffects;
import com.bitfire.uracer.effects.TrackEffects.Effects;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.game.logic.DriftInfo;
import com.bitfire.uracer.game.logic.GameLogic;
import com.bitfire.uracer.messager.Messager;
import com.bitfire.uracer.simulations.car.Replay;

public class Hud
{
	private GameLogic logic;
	private Car player;

	private HudLabel best, curr, last;
	private HudDebugMeter meterLatForce, meterSkidMarks;
	private Matrix4 topLeftOrigin, identity;

	// effects
	public Hud( GameLogic logic )
	{
		this.logic = logic;
		player = logic.getGame().getLevel().getPlayer();

		// y-flip
		topLeftOrigin = new Matrix4();
		topLeftOrigin.setToOrtho( 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0, 0, 10 );
		identity = new Matrix4();

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
		meterLatForce.setLimits( 0, 1 );
		meterLatForce.setName( "lat-force-FRONT" );

		// meter skid marks count
		meterSkidMarks = new HudDebugMeter( this, 1, 100, 5 );
		meterSkidMarks.setLimits( 0, CarSkidMarks.MaxSkidMarks );
		meterSkidMarks.setName( "skid marks count" );
	}

	public GameLogic getLogic()
	{
		return logic;
	}

	public void dispose()
	{
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
			{
				best.setString( "BEST TIME\n-.----" );
			}
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

	public void render( SpriteBatch batch )
	{
		batch.setTransformMatrix( identity );
		batch.setProjectionMatrix( topLeftOrigin );
		batch.begin();

		Messager.render( batch );

		updateLapTimes();
		curr.render( batch );
		best.render( batch );
		last.render( batch );

		if( Config.isDesktop )
		{
			DriftInfo drift = DriftInfo.get();

			// lateral forces
			meterLatForce.setValue( drift.driftStrength );
			if(drift.isDrifting) meterLatForce.color.set( .3f, 1f, .3f, 1f );
			else meterLatForce.color.set( 1f, 1f, 1f, 1f );
			meterLatForce.render( batch );

			meterSkidMarks.setValue( TrackEffects.getParticleCount( Effects.CarSkidMarks ) );
			meterSkidMarks.render( batch );
		}

		batch.end();
	}

	public void debug()
	{
		meterLatForce.debug();
		meterSkidMarks.debug();
	}
}
