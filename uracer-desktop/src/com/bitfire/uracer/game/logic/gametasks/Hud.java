
package com.bitfire.uracer.game.logic.gametasks;

import com.bitfire.uracer.events.GameRendererEvent;
import com.bitfire.uracer.events.GameRendererEvent.Order;
import com.bitfire.uracer.events.GameRendererEvent.Type;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.logic.gametasks.hud.HudElement;
import com.bitfire.utils.ItemsManager;

/** Encapsulates an head-up manager that will callback HudElement events for their updating and drawing operations. */
public final class Hud extends GameTask implements DisposableTasks {

	private static final GameRendererEvent.Type RenderEventBeforePost = GameRendererEvent.Type.BatchBeforePostProcessing;
	private static final GameRendererEvent.Type RenderEventAfterPost = GameRendererEvent.Type.BatchAfterPostProcessing;
	private static final GameRendererEvent.Type RenderEventDebug = GameRendererEvent.Type.BatchDebug;

	private final ItemsManager<HudElement> managerBeforePost = new ItemsManager<HudElement>();
	private final ItemsManager<HudElement> managerAfterPost = new ItemsManager<HudElement>();
	private final ItemsManager<HudElement> managerDebug = new ItemsManager<HudElement>();

	private final GameRendererEvent.Listener renderEvent = new GameRendererEvent.Listener() {
		@Override
		public void handle (Object source, Type type, Order order) {
			if (order != GameRendererEvent.Order.DEFAULT) {
				return;
			}

			if (type == Type.BatchBeforePostProcessing) {
				for (HudElement e : managerBeforePost) {
					e.onRender(GameEvents.gameRenderer.batch);
				}
			} else if (type == Type.BatchAfterPostProcessing) {
				for (HudElement e : managerAfterPost) {
					e.onRender(GameEvents.gameRenderer.batch);
				}
			} else if (type == Type.BatchDebug) {
				for (HudElement e : managerDebug) {
					e.onRender(GameEvents.gameRenderer.batch);
				}
			}
		}
	};

	public Hud () {
		GameEvents.gameRenderer.addListener(renderEvent, RenderEventBeforePost, GameRendererEvent.Order.DEFAULT);
		GameEvents.gameRenderer.addListener(renderEvent, RenderEventAfterPost, GameRendererEvent.Order.DEFAULT);
		GameEvents.gameRenderer.addListener(renderEvent, RenderEventDebug, GameRendererEvent.Order.DEFAULT);
	}

	public void addBeforePostProcessing (HudElement element) {
		managerBeforePost.add(element);
	}

	public void addAfterPostProcessing (HudElement element) {
		managerAfterPost.add(element);
	}

	public void addDebug (HudElement element) {
		managerDebug.add(element);
	}

	public void remove (HudElement element) {
		managerBeforePost.remove(element);
		managerAfterPost.remove(element);
		managerDebug.remove(element);
	}

	@Override
	public void dispose () {
		super.dispose();
		GameEvents.gameRenderer.removeListener(renderEvent, RenderEventBeforePost, GameRendererEvent.Order.DEFAULT);
		GameEvents.gameRenderer.removeListener(renderEvent, RenderEventAfterPost, GameRendererEvent.Order.DEFAULT);
		GameEvents.gameRenderer.removeListener(renderEvent, RenderEventDebug, GameRendererEvent.Order.DEFAULT);
		disposeTasks();
	}

	@Override
	public void disposeTasks () {
		managerBeforePost.dispose();
		managerAfterPost.dispose();
		managerDebug.dispose();
	}

	@Override
	public void onRestart () {
		for (HudElement e : managerBeforePost) {
			e.onRestart();
		}

		for (HudElement e : managerAfterPost) {
			e.onRestart();
		}

		for (HudElement e : managerDebug) {
			e.onRestart();
		}
	}

	@Override
	public void onReset () {
		for (HudElement e : managerBeforePost) {
			e.onReset();
		}

		for (HudElement e : managerAfterPost) {
			e.onReset();
		}

		for (HudElement e : managerDebug) {
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

		for (HudElement e : managerDebug) {
			e.onTick();
		}
	}
}
