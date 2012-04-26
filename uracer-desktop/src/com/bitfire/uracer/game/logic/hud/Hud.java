package com.bitfire.uracer.game.logic.hud;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.game.events.GameEvents;
import com.bitfire.uracer.game.events.GameRendererEvent;
import com.bitfire.uracer.game.logic.GameTask;
import com.bitfire.uracer.utils.Manager;

// FIXME should extrapolate the lap times thing.. this is just a fucking manager
public final class Hud extends GameTask {

	private final Manager<HudElement> manager = new Manager<HudElement>();

	private GameRendererEvent.Listener gameRendererEvent = new GameRendererEvent.Listener() {
		@Override
		public void gameRendererEvent( GameRendererEvent.Type type ) {
			renderAfterMeshes( GameEvents.gameRenderer.batch );
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
}
