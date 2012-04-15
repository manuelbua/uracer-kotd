package com.bitfire.uracer.game.logic.hud;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.Replay;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.data.GameData;
import com.bitfire.uracer.game.events.GameLogicEvent;
import com.bitfire.uracer.game.events.GameRendererEvent;
import com.bitfire.uracer.game.states.LapState;
import com.bitfire.uracer.task.Task;
import com.bitfire.uracer.utils.Manager;
import com.bitfire.uracer.utils.NumberString;

public final class Hud extends Task {

	private final Manager<HudElement> manager = new Manager<HudElement>();

	private HudLabel best, curr, last;
	// private HudDebugMeter meterLatForce, meterSkidMarks, meterSmoke;

	private Vector2 playerPosition = new Vector2();
	private float playerOrientation;

	private GameRendererEvent.Listener gameRendererEvent = new GameRendererEvent.Listener() {
		@Override
		public void gameRendererEvent( GameRendererEvent.Type type ) {
			SpriteBatch batch = GameEvents.gameRenderer.batch;

			switch( type ) {
			case BatchAfterMeshes:
				renderAfterMeshes( batch );
				break;
			case BatchDebug:
				if( Config.Graphics.RenderHudDebugInfo ) {
					onDebug( batch );
				}
				break;
			}
		}
	};

	private void renderAfterMeshes( SpriteBatch batch ) {
		Array<HudElement> items = manager.items;
		for( int i = 0; i < items.size; i++ ) {
			items.get( i ).onRender( batch, playerPosition, playerOrientation );
		}

		curr.render( batch );
		best.render( batch );
		last.render( batch );
	}

	private GameLogicEvent.Listener gameLogicEvent = new GameLogicEvent.Listener() {
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
		GameEvents.gameLogic.addListener( gameLogicEvent, GameLogicEvent.Type.onReset );
		GameEvents.gameLogic.addListener( gameLogicEvent, GameLogicEvent.Type.onRestart );

		// grid-based position
		int gridX = (int)((float)Gdx.graphics.getWidth() / 5f);

		// laptimes component
		best = new HudLabel( Art.fontCurseYR, "BEST  TIME\n-.----" );
		curr = new HudLabel( Art.fontCurseYR, "YOUR  TIME\n-.----" );
		last = new HudLabel( Art.fontCurseYR, "LAST  TIME\n-.----" );

		curr.setPosition( gridX, 50 * GameData.Environment.scalingStrategy.invTileMapZoomFactor );
		last.setPosition( gridX * 3, 50 * GameData.Environment.scalingStrategy.invTileMapZoomFactor );
		best.setPosition( gridX * 4, 50 * GameData.Environment.scalingStrategy.invTileMapZoomFactor );

		// // meter lateral forces
		// meterLatForce = new HudDebugMeter( car, 0, 100, 5 );
		// meterLatForce.setLimits( 0, 1 );
		// meterLatForce.setName( "lat-force-FRONT" );
		//
		// // meter skid marks count
		// meterSkidMarks = new HudDebugMeter( car, 1, 100, 5 );
		// meterSkidMarks.setLimits( 0, CarSkidMarks.MaxSkidMarks );
		// meterSkidMarks.setName( "skid marks count" );
		//
		// meterSmoke = new HudDebugMeter( car, 2, 100, 5 );
		// meterSmoke.setLimits( 0, SmokeTrails.MaxParticles );
		// meterSmoke.setName( "smokepar count" );
	}

	public void add( HudElement element ) {
		manager.add( element );
	}

	public void remove( HudElement element ) {
		manager.remove( element );
	}

	@Override
	public void dispose() {
		super.dispose();
		manager.dispose();
	}

	public void reset() {
		Array<HudElement> items = manager.items;
		for( int i = 0; i < items.size; i++ ) {
			items.get( i ).onReset();
		}
	}

	public void trackPlayerPosition( Car car ) {
		playerPosition.set( car.state().position );
		playerOrientation = car.state().orientation;

		// no subframe
		// playerPosition.set( Convert.mt2px(car.getBody().getPosition()) );
		// playerOrientation = car.getBody().getAngle() * MathUtils.radiansToDegrees;
	}

	@Override
	protected void onTick() {
		for( int i = 0; i < manager.items.size; i++ ) {
			manager.items.get( i ).onTick();
		}
	}

	public void update( LapState lapState ) {

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

	// FIXME, create a DebugHud as a debug aid instead..
	public void onDebug( SpriteBatch batch ) {
		// DriftState drift = GameData.States.driftState;

		// // lateral forces
		// meterLatForce.setValue( drift.driftStrength );
		//
		// if( drift.isDrifting ) {
		// meterLatForce.color.set( .3f, 1f, .3f, 1f );
		// } else {
		// meterLatForce.color.set( 1f, 1f, 1f, 1f );
		// }
		//
		// meterLatForce.render( batch );

		// meterSkidMarks.setValue( GameData.Systems.trackEffects.getParticleCount( Type.CarSkidMarks ) );
		// meterSkidMarks.render( batch );
		//
		// meterSmoke.setValue( GameData.Systems.trackEffects.getParticleCount( Type.SmokeTrails ) );
		// meterSmoke.render( batch );
	}
}
