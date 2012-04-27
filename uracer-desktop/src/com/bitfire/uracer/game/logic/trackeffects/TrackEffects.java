package com.bitfire.uracer.game.logic.trackeffects;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.game.events.GameEvents;
import com.bitfire.uracer.game.logic.GameTask;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.rendering.GameRendererEvent;
import com.bitfire.uracer.utils.Manager;

public final class TrackEffects extends GameTask {
	private Manager<TrackEffect> manager = new Manager<TrackEffect>();

	private final GameRendererEvent.Listener gameRendererEvent = new GameRendererEvent.Listener() {
		@Override
		public void gameRendererEvent( GameRendererEvent.Type type ) {
			GameRenderer gameRenderer = GameEvents.gameRenderer.source;

			SpriteBatch batch = GameEvents.gameRenderer.batch;
			Array<TrackEffect> items = manager.items;

			for( int i = 0; i < items.size; i++ ) {
				TrackEffect effect = items.get( i );
				if( effect != null ) {
					effect.render( gameRenderer, batch );
				}
			}
		}
	};

	public TrackEffects() {
		GameEvents.gameRenderer.addListener( gameRendererEvent, GameRendererEvent.Type.BatchBeforeMeshes, GameRendererEvent.Order.MINUS_4 );

		// NOTE for custom render event
		// for CarSkidMarks GameRenderer.event.addListener( gameRendererEvent, GameRendererEvent.Type.BatchBeforeMeshes,
		// GameRendererEvent.Order.Order_Minus_4 );
		// for SmokeTrails GameRenderer.event.addListener( gameRendererEvent, GameRendererEvent.Type.BatchBeforeMeshes,
		// GameRendererEvent.Order.Order_Minus_3 );
	}

	public void add( TrackEffect effect ) {
		manager.add( effect );
	}

	public void remove( TrackEffect effect ) {
		manager.remove( effect );
	}

	@Override
	public void dispose() {
		super.dispose();
		manager.dispose();
	}

	@Override
	public void onTick() {
		Array<TrackEffect> items = manager.items;
		for( int i = 0; i < items.size; i++ ) {
			TrackEffect effect = items.get( i );
			effect.tick();
		}
	}

	@Override
	public void onReset() {
		Array<TrackEffect> items = manager.items;
		for( int i = 0; i < items.size; i++ ) {
			TrackEffect effect = items.get( i );
			effect.reset();
		}
	}

	public int getParticleCount() {
		Array<TrackEffect> items = manager.items;
		int total = 0;
		for( int i = 0; i < items.size; i++ ) {
			TrackEffect effect = items.get( i );
			total += effect.getParticleCount();
		}

		return total;
	}
}
