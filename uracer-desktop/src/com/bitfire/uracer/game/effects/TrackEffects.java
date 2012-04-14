package com.bitfire.uracer.game.effects;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.events.GameLogicEvent;
import com.bitfire.uracer.game.events.GameRendererEvent;
import com.bitfire.uracer.task.Task;

public final class TrackEffects extends Task {
	private Array<TrackEffect> effects = new Array<TrackEffect>();
	private Array<Boolean> owned = new Array<Boolean>();

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
			for( int i = 0; i < effects.size; i++ ) {
				TrackEffect effect = effects.get( i );
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

	/** Add a new effect to the manager, if own is true the manager will manage its lifecycle */
	public void add(TrackEffect effect, boolean own) {
		effects.add( effect );
		owned.add(own);
	}

	/** Add a new effect to the manager, and transfer resource's ownership to it */
	public void add(TrackEffect effect) {
		add(effect, true);
	}

	public void remove(TrackEffect effect) {
		int index = effects.indexOf( effect, true );
		effects.removeIndex( index );
		owned.removeIndex( index );
	}

	@Override
	public void onTick() {
		for( int i = 0; i < effects.size; i++ ) {
			TrackEffect effect = effects.get( i );
			if( effect != null ) {
				effect.onTick();
			}
		}
	}

	public void reset() {
		for( int i = 0; i < effects.size; i++ ) {
			TrackEffect effect = effects.get( i );
			if( effect != null ) {
				effect.reset();
			}
		}
	}

	@Override
	public void dispose() {
		for( int i = 0; i < effects.size; i++ ) {
			TrackEffect effect = effects.get( i );
			if( effect != null ) {
				effect.dispose();
			}
		}

		effects = null;
	}

	public int getParticleCount() {
		int total = 0;
		for( int i = 0; i < effects.size; i++ ) {
			TrackEffect effect = effects.get( i );
			if( effect != null ) {
				total += effect.getParticleCount();
			}
		}

		return total;
	}
}
