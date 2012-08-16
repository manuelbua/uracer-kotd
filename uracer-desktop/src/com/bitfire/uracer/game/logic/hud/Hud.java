
package com.bitfire.uracer.game.logic.hud;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.logic.GameTask;
import com.bitfire.uracer.game.rendering.GameRendererEvent;
import com.bitfire.uracer.game.rendering.GameRendererEvent.Order;
import com.bitfire.uracer.game.rendering.GameRendererEvent.Type;
import com.bitfire.utils.ItemsManager;

/** Encapsulates an head-up manager that will callback HudElement events for their updating and drawing operations. */
public final class Hud extends GameTask {

	private final ItemsManager<HudElement> managerAfterMeshes = new ItemsManager<HudElement>();
	private final ItemsManager<HudElement> managerAfterPost = new ItemsManager<HudElement>();

	private GameRendererEvent.Listener gameRendererEvent = new GameRendererEvent.Listener() {
		@Override
		public void gameRendererEvent (GameRendererEvent.Type type, Order order) {
			if (order == Order.DEFAULT && type == Type.BatchAfterMeshes) {
				renderAfterMeshes(GameEvents.gameRenderer.batch);
			} else if (order == Order.DEFAULT && type == Type.BatchAfterPostProcessing) {
				renderAfterPostProcessing(GameEvents.gameRenderer.batch);
			}
		}
	};

	private void renderAfterMeshes (SpriteBatch batch) {
		Array<HudElement> items = managerAfterMeshes.items;
		for (int i = 0; i < items.size; i++) {
			items.get(i).onRender(batch);
		}
	}

	private void renderAfterPostProcessing (SpriteBatch batch) {
		Array<HudElement> items = managerAfterPost.items;
		for (int i = 0; i < items.size; i++) {
			items.get(i).onRender(batch);
		}
	}

	// effects
	public Hud () {
		GameEvents.gameRenderer.addListener(gameRendererEvent, GameRendererEvent.Type.BatchAfterMeshes,
			GameRendererEvent.Order.DEFAULT);
		GameEvents.gameRenderer.addListener(gameRendererEvent, GameRendererEvent.Type.BatchAfterPostProcessing,
			GameRendererEvent.Order.DEFAULT);
	}

	public void addAfterMeshes (HudElement element) {
		managerAfterMeshes.add(element);
	}

	public void addAfterPostProcessing (HudElement element) {
		managerAfterPost.add(element);
	}

	public void remove (HudElement element) {
		managerAfterMeshes.remove(element);
		managerAfterPost.remove(element);
	}

	@Override
	public void dispose () {
		super.dispose();
		GameEvents.gameRenderer.removeListener(gameRendererEvent, GameRendererEvent.Type.BatchAfterMeshes,
			GameRendererEvent.Order.DEFAULT);
		GameEvents.gameRenderer.removeListener(gameRendererEvent, GameRendererEvent.Type.BatchAfterPostProcessing,
			GameRendererEvent.Order.DEFAULT);
		managerAfterMeshes.dispose();
		managerAfterPost.dispose();
	}

	@Override
	public void onReset () {
		for (int i = 0; i < managerAfterMeshes.items.size; i++) {
			managerAfterMeshes.items.get(i).onReset();
		}
		for (int i = 0; i < managerAfterPost.items.size; i++) {
			managerAfterPost.items.get(i).onReset();
		}
	}

	@Override
	protected void onTick () {
		for (int i = 0; i < managerAfterMeshes.items.size; i++) {
			managerAfterMeshes.items.get(i).onTick();
		}

		for (int i = 0; i < managerAfterPost.items.size; i++) {
			managerAfterPost.items.get(i).onTick();
		}
	}
}
