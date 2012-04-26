package com.bitfire.uracer.game.logic.hud;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.game.events.GameEvents;
import com.bitfire.uracer.game.events.GameRendererEvent;
import com.bitfire.uracer.game.logic.GameTask;
import com.bitfire.uracer.utils.Manager;

// FIXME should extrapolate the lap times thing.. this is just a fucking manager
public final class Hud extends GameTask {

	private final Manager<HudElement> manager = new Manager<HudElement>();

	// private HudDebugMeter meterLatForce, meterSkidMarks, meterSmoke;

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
			items.get( i ).onRender( batch );
		}
	}

	// effects
	public Hud( ScalingStrategy scalingStrategy ) {
		GameEvents.gameRenderer.addListener( gameRendererEvent, GameRendererEvent.Type.BatchAfterMeshes, GameRendererEvent.Order.DEFAULT );
		GameEvents.gameRenderer.addListener( gameRendererEvent, GameRendererEvent.Type.BatchDebug, GameRendererEvent.Order.DEFAULT );

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

	@Override
	public void onReset() {
		Array<HudElement> items = manager.items;
		for( int i = 0; i < items.size; i++ ) {
			items.get( i ).onReset();
		}
	}

	@Override
	protected void onTick() {
		for( int i = 0; i < manager.items.size; i++ ) {
			manager.items.get( i ).onTick();
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
