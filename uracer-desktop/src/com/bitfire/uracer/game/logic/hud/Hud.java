package com.bitfire.uracer.game.logic.hud;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.logic.GameTask;
import com.bitfire.uracer.game.rendering.GameRendererEvent;
import com.bitfire.uracer.game.rendering.GameRendererEvent.Order;
import com.bitfire.uracer.utils.Manager;

/** Encapsulates an head-up manager that will callback HudElement events for
 * their updating and drawing operations. */
public final class Hud extends GameTask {

	private final Manager<HudElement> manager = new Manager<HudElement>();

	private GameRendererEvent.Listener gameRendererEvent = new GameRendererEvent.Listener() {
		@Override
		public void gameRendererEvent( GameRendererEvent.Type type, Order order ) {
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
	public Hud() {
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
		GameEvents.gameRenderer.removeListener( gameRendererEvent, GameRendererEvent.Type.BatchAfterMeshes, GameRendererEvent.Order.DEFAULT );
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
