
package com.bitfire.uracer.game.logic.gametasks;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.events.GameRendererEvent;
import com.bitfire.uracer.game.events.GameRendererEvent.Order;
import com.bitfire.uracer.game.events.GameRendererEvent.Type;
import com.bitfire.uracer.game.logic.gametasks.hud.HudElement;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.utils.ItemsManager;

/** Encapsulates an head-up manager that will callback HudElement events for their updating and drawing operations. */
public final class Hud extends GameTask implements DisposableTasks {

	private static final GameRendererEvent.Type RenderEventBeforePost = GameRendererEvent.Type.BatchBeforePostProcessing;
	private static final GameRendererEvent.Type RenderEventAfterPost = GameRendererEvent.Type.BatchAfterPostProcessing;

	private final ItemsManager<HudElement> managerBeforePost = new ItemsManager<HudElement>();
	private final ItemsManager<HudElement> managerAfterPost = new ItemsManager<HudElement>();

	private final GameRendererEvent.Listener renderEvent = new GameRendererEvent.Listener() {
		@Override
		public void handle (Object source, Type type, Order order) {
			if (order != GameRendererEvent.Order.DEFAULT) {
				return;
			}

			SpriteBatch batch = GameEvents.gameRenderer.batch;
			float camZoom = GameEvents.gameRenderer.camZoom;
			ItemsManager<HudElement> items = null;

			if (type == Type.BatchBeforePostProcessing) {
				items = managerBeforePost;
			} else if (type == Type.BatchAfterPostProcessing) {
				items = managerAfterPost;
			}

			if (items != null) {
				for (HudElement e : items) {
					e.onRender(batch, camZoom);
				}
			}
		}
	};

	public Hud () {
		GameEvents.gameRenderer.addListener(renderEvent, RenderEventBeforePost, GameRendererEvent.Order.DEFAULT);
		GameEvents.gameRenderer.addListener(renderEvent, RenderEventAfterPost, GameRendererEvent.Order.DEFAULT);
	}

	public void addBeforePostProcessing (HudElement element) {
		managerBeforePost.add(element);
	}

	public void addAfterPostProcessing (HudElement element) {
		managerAfterPost.add(element);
	}

	public void remove (HudElement element) {
		managerBeforePost.remove(element);
		managerAfterPost.remove(element);
	}

	@Override
	public void dispose () {
		super.dispose();
		GameEvents.gameRenderer.removeListener(renderEvent, RenderEventBeforePost, GameRendererEvent.Order.DEFAULT);
		GameEvents.gameRenderer.removeListener(renderEvent, RenderEventAfterPost, GameRendererEvent.Order.DEFAULT);
		disposeTasks();
	}

	@Override
	public void disposeTasks () {
		managerBeforePost.dispose();
		managerAfterPost.dispose();
	}

	@Override
	public void onGameRestart () {
		for (HudElement e : managerBeforePost) {
			e.onRestart();
		}

		for (HudElement e : managerAfterPost) {
			e.onRestart();
		}
	}

	@Override
	public void onGameReset () {
		for (HudElement e : managerBeforePost) {
			e.onReset();
		}

		for (HudElement e : managerAfterPost) {
			e.onReset();
		}
	}

	@Override
	protected void onTick () {
		for (HudElement e : managerBeforePost) {
			e.onTick();
		}

		for (HudElement e : managerAfterPost) {
			e.onTick();
		}
	}

	@Override
	public void onPlayer (PlayerCar player) {
		super.onPlayer(player);

		for (HudElement e : managerBeforePost) {
			e.player(player);
		}

		for (HudElement e : managerAfterPost) {
			e.player(player);
		}
	}
}
