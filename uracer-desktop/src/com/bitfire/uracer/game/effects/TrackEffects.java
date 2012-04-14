package com.bitfire.uracer.game.effects;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.Manager;
import com.bitfire.uracer.game.events.GameLogicEvent;
import com.bitfire.uracer.game.events.GameRendererEvent;

public final class TrackEffects extends Manager<TrackEffect> {
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

	private final GameRendererEvent.Listener gameRendererEvent = new GameRendererEvent.Listener() {
		@Override
		public void gameRendererEvent( GameRendererEvent.Type type ) {
			SpriteBatch batch = GameEvents.gameRenderer.batch;
			for( int i = 0; i < items.size; i++ ) {
				TrackEffect effect = items.get( i );
				if( effect != null ) {
					effect.render( batch );
				}
			}
		}
	};

	public TrackEffects() {
		GameEvents.gameLogic.addListener( gameLogicEvent );
		GameEvents.gameRenderer.addListener( gameRendererEvent, GameRendererEvent.Type.BatchBeforeMeshes, GameRendererEvent.Order.MINUS_4 );

		// TODO, custom render event
		// for CarSkidMarks GameRenderer.event.addListener( gameRendererEvent, GameRendererEvent.Type.BatchBeforeMeshes,
		// GameRendererEvent.Order.Order_Minus_4 );
		// for SmokeTrails GameRenderer.event.addListener( gameRendererEvent, GameRendererEvent.Type.BatchBeforeMeshes,
		// GameRendererEvent.Order.Order_Minus_3 );
	}



	@Override
	public void onTick() {
		for( int i = 0; i < items.size; i++ ) {
			TrackEffect effect = items.get( i );
			effect.onTick();
		}
	}

	public void reset() {
		for( int i = 0; i < items.size; i++ ) {
			TrackEffect effect = items.get( i );
			effect.reset();
		}
	}

	public int getParticleCount() {
		int total = 0;
		for( int i = 0; i < items.size; i++ ) {
			TrackEffect effect = items.get( i );
			total += effect.getParticleCount();
		}

		return total;
	}
}
