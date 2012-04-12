package com.bitfire.uracer.game.hud;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.Replay;
import com.bitfire.uracer.game.data.GameData;
import com.bitfire.uracer.game.effects.CarSkidMarks;
import com.bitfire.uracer.game.effects.SmokeTrails;
import com.bitfire.uracer.game.effects.TrackEffect.Type;
import com.bitfire.uracer.game.events.GameLogicEvent;
import com.bitfire.uracer.game.events.GameRendererEvent;
import com.bitfire.uracer.game.player.Car;
import com.bitfire.uracer.game.states.DriftState;
import com.bitfire.uracer.game.states.LapState;
import com.bitfire.uracer.task.Task;
import com.bitfire.uracer.utils.NumberString;

public class Hud extends Task {
	private HudLabel best, curr, last;
	private HudDebugMeter meterLatForce, meterSkidMarks, meterSmoke;

	// components
	private HudDrifting hudDrift;

	private final GameRendererEvent.Listener gameRendererEvent = new GameRendererEvent.Listener() {
		@Override
		public void gameRendererEvent( GameRendererEvent.Type type ) {
			SpriteBatch batch = GameEvents.gameRenderer.batch;

			switch( type ) {
			case BatchAfterMeshes:
				curr.render( batch );
				best.render( batch );
				last.render( batch );

				// render drifting component
				hudDrift.render( batch );
				break;
			case BatchDebug:
				if( Config.Graphics.RenderHudDebugInfo ) {
					onDebug( batch );
				}
				break;
			}
		}
	};

	private final GameLogicEvent.Listener gameLogicEvent = new GameLogicEvent.Listener() {
		@Override
		public void gameLogicEvent( com.bitfire.uracer.game.events.GameLogicEvent.Type type ) {
			switch( type ) {
			case onReset:
			case onRestart:
				reset();
				break;
			}
		}
	};

	// effects
	public Hud() {
		GameEvents.gameRenderer.addListener( gameRendererEvent, GameRendererEvent.Type.BatchAfterMeshes, GameRendererEvent.Order.DEFAULT );
		GameEvents.gameRenderer.addListener( gameRendererEvent, GameRendererEvent.Type.BatchDebug, GameRendererEvent.Order.DEFAULT );
		GameEvents.gameLogic.addListener( gameLogicEvent );

		// grid-based position
		int gridX = (int)((float)Gdx.graphics.getWidth() / 5f);

		// laptimes component
		best = new HudLabel( Art.fontCurseYR, "BEST  TIME\n-.----" );
		curr = new HudLabel( Art.fontCurseYR, "YOUR  TIME\n-.----" );
		last = new HudLabel( Art.fontCurseYR, "LAST  TIME\n-.----" );

		Car car = GameData.States.playerState.car;

		// drifting component
		hudDrift = new HudDrifting( car );

		curr.setPosition( gridX, 50 );
		last.setPosition( gridX * 3, 50 );
		best.setPosition( gridX * 4, 50 );

		// meter lateral forces
		meterLatForce = new HudDebugMeter( car, 0, 100, 5 );
		meterLatForce.setLimits( 0, 1 );
		meterLatForce.setName( "lat-force-FRONT" );

		// meter skid marks count
		meterSkidMarks = new HudDebugMeter( car, 1, 100, 5 );
		meterSkidMarks.setLimits( 0, CarSkidMarks.MaxSkidMarks );
		meterSkidMarks.setName( "skid marks count" );

		meterSmoke = new HudDebugMeter( car, 2, 100, 5 );
		meterSmoke.setLimits( 0, SmokeTrails.MaxParticles );
		meterSmoke.setName( "smokepar count" );
	}

	public void reset() {
		hudDrift.reset();
	}

	@Override
	protected void onTick() {
		hudDrift.tick();
		updateLapTimes();
	}

	private void updateLapTimes() {

		LapState lapState = GameData.States.lapState;

		// current time
		curr.setString( "YOUR  TIME\n" + NumberString.format( lapState.getElapsedSeconds() ) + "s" );

		// render best lap time
		Replay rbest = lapState.getBestReplay();

		// best time
		if( rbest != null && rbest.isValid ) {
			// has best
			best.setString( "BEST  TIME\n" + NumberString.format( rbest.trackTimeSeconds ) + "s" );
		} else {
			// temporarily use last track time
			if( lapState.hasLastTrackTimeSeconds() ) {
				best.setString( "BEST  TIME\n" + NumberString.format( lapState.getLastTrackTimeSeconds() ) + "s" );
			} else {
				best.setString( "BEST TIME\n-:----" );
			}
		}

		// last time
		if( lapState.hasLastTrackTimeSeconds() ) {
			// has only last
			last.setString( "LAST  TIME\n" + NumberString.format( lapState.getLastTrackTimeSeconds() ) + "s" );
		} else {
			last.setString( "LAST  TIME\n-:----" );
		}
	}

	public void onDebug( SpriteBatch batch ) {
		DriftState drift = GameData.States.driftState;

		// lateral forces
		meterLatForce.setValue( drift.driftStrength );

		if( drift.isDrifting ) {
			meterLatForce.color.set( .3f, 1f, .3f, 1f );
		} else {
			meterLatForce.color.set( 1f, 1f, 1f, 1f );
		}

		meterLatForce.render( batch );

		meterSkidMarks.setValue( GameData.Systems.trackEffects.getParticleCount( Type.CarSkidMarks ) );
		meterSkidMarks.render( batch );

		meterSmoke.setValue( GameData.Systems.trackEffects.getParticleCount( Type.SmokeTrails ) );
		meterSmoke.render( batch );
	}
}
