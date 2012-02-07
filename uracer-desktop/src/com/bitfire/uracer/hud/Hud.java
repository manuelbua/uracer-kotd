package com.bitfire.uracer.hud;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.carsimulation.Replay;
import com.bitfire.uracer.effects.CarSkidMarks;
import com.bitfire.uracer.effects.SmokeTrails;
import com.bitfire.uracer.effects.TrackEffects;
import com.bitfire.uracer.effects.TrackEffects.Effects;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.game.Game;
import com.bitfire.uracer.game.logic.DriftInfo;
import com.bitfire.uracer.game.logic.LapInfo;
import com.bitfire.uracer.messager.Messager;
import com.bitfire.uracer.utils.FloatString;

public class Hud
{
	private Game game;
	private Car player;

	private HudLabel best, curr, last;
	private Matrix4 topLeftOrigin, identity;
	private HudDebugMeter meterLatForce, meterSkidMarks, meterSmoke;

	// components
	private HudDrifting hudDrift;

	// effects
	public Hud( Game game )
	{
		this.game = game;
		player = game.getPlayer();

		// y-flip
		topLeftOrigin = new Matrix4();
		topLeftOrigin.setToOrtho( 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0, 0, 10 );
		identity = new Matrix4();

		// grid-based position
		int gridX = (int)((float)Gdx.graphics.getWidth() / 5f);

		// laptimes component
		best = new HudLabel( Art.fontCurseYR, "BEST  TIME\n-.----" );
		curr = new HudLabel( Art.fontCurseYR, "YOUR  TIME\n-.----" );
		last = new HudLabel( Art.fontCurseYR, "LAST  TIME\n-.----" );

		// drifting component
		hudDrift = new HudDrifting( game );

		curr.setPosition( gridX, 50 );
		last.setPosition( gridX * 3, 50 );
		best.setPosition( gridX * 4, 50 );

		// meter lateral forces
		meterLatForce = new HudDebugMeter( game, 0, 100, 5 );
		meterLatForce.setLimits( 0, 1 );
		meterLatForce.setName( "lat-force-FRONT" );

		// meter skid marks count
		meterSkidMarks = new HudDebugMeter( game, 1, 100, 5 );
		meterSkidMarks.setLimits( 0, CarSkidMarks.MaxSkidMarks );
		meterSkidMarks.setName( "skid marks count" );

		meterSmoke = new HudDebugMeter( game, 2, 100, 5 );
		meterSmoke.setLimits( 0, SmokeTrails.MaxParticles );
		meterSmoke.setName( "smokepar count" );
	}

	public void dispose()
	{
	}

	public void tick()
	{
		Messager.tick();
		hudDrift.tick();
	}

	private void updateLapTimes()
	{
		LapInfo lapInfo = LapInfo.get();

		// current time
		curr.setString( "YOUR  TIME\n" + FloatString.format(lapInfo.getElapsedSeconds()) + "s" );

		// render best lap time
		Replay rbest = lapInfo.getBestReplay();

		// best time
		if( rbest != null && rbest.isValid )
		{
			// has best
			best.setString( "BEST  TIME\n" + FloatString.format(rbest.trackTimeSeconds) + "s" );
		} else
		{
			// temporarily use last track time
			if( lapInfo.hasLastTrackTimeSeconds() )
			{
				best.setString( "BEST  TIME\n" + FloatString.format(lapInfo.getLastTrackTimeSeconds()) + "s" );
			} else
			{
				best.setString( "BEST TIME\n-:----" );
			}
		}

		// last time
		if( lapInfo.hasLastTrackTimeSeconds() )
		{
			// has only last
			last.setString( "LAST  TIME\n" + FloatString.format(lapInfo.getLastTrackTimeSeconds()) + "s" );
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

		// render drifting component
		hudDrift.render( batch );

		batch.end();
	}

	public void debug( SpriteBatch batch )
	{
		meterLatForce.debug();
		meterSkidMarks.debug();
		meterSmoke.debug();

		DriftInfo drift = DriftInfo.get();

		// lateral forces
		meterLatForce.setValue( drift.driftStrength );

		if( drift.isDrifting )
			meterLatForce.color.set( .3f, 1f, .3f, 1f );
		else
			meterLatForce.color.set( 1f, 1f, 1f, 1f );

		meterLatForce.render( batch );

		meterSkidMarks.setValue( TrackEffects.getParticleCount( Effects.CarSkidMarks ) );
		meterSkidMarks.render( batch );

		meterSmoke.setValue( TrackEffects.getParticleCount( Effects.SmokeTrails ) );
		meterSmoke.render( batch );
	}


	/**
	 * Expose components
	 * TODO find a better way for this
	 * @return
	 */

	public HudDrifting getDrifting()
	{
		return hudDrift;
	}
}
