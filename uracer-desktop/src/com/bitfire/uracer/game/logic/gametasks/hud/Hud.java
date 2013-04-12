
package com.bitfire.uracer.game.logic.gametasks.hud;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.events.GameRendererEvent;
import com.bitfire.uracer.events.GameRendererEvent.Order;
import com.bitfire.uracer.events.GameRendererEvent.Type;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.logic.gametasks.GameTask;
import com.bitfire.utils.ItemsManager;

/** Encapsulates an head-up manager that will callback HudElement events for their updating and drawing operations. */
public final class Hud extends GameTask implements GameRendererEvent.Listener {

	private static final GameRendererEvent.Type RenderEventBeforePost = GameRendererEvent.Type.BatchAfterMeshes;
	private static final GameRendererEvent.Type RenderEventAfterPost = GameRendererEvent.Type.BatchAfterPostProcessing;
	private static final GameRendererEvent.Order RenderOrderBeforePost = GameRendererEvent.Order.DEFAULT;
	private static final GameRendererEvent.Order RenderOrderAfterPost = GameRendererEvent.Order.DEFAULT;

	private final ItemsManager<HudElement> managerAfterMeshes = new ItemsManager<HudElement>();
	private final ItemsManager<HudElement> managerAfterPost = new ItemsManager<HudElement>();

	private ScalingStrategy strategy = null;
	private float scale = 1f;
	private final Matrix4 identity = new Matrix4();
	private final Matrix4 xform = new Matrix4();

	public Hud (ScalingStrategy strategy) {
		this.strategy = strategy;
		GameEvents.gameRenderer.addListener(this, RenderEventBeforePost, RenderOrderBeforePost);
		GameEvents.gameRenderer.addListener(this, RenderEventAfterPost, RenderOrderAfterPost);
	}

	@Override
	public void gameRendererEvent (GameRendererEvent.Type type, Order order) {

		SpriteBatch batch = GameEvents.gameRenderer.batch;

		if (batch != null) {
			float sx = Gdx.graphics.getWidth() / strategy.referenceResolution.x;
			float sy = Gdx.graphics.getHeight() / strategy.referenceResolution.y;

			xform.idt();
			xform.scale(sx, sy, 1);

			// xform.scale(1, 1, 1);

			batch.setTransformMatrix(xform);
		}

		if (order == Order.DEFAULT && type == Type.BatchAfterMeshes) {

			Array<HudElement> items = managerAfterMeshes.items;
			for (int i = 0; i < items.size; i++) {
				items.get(i).onRender(GameEvents.gameRenderer.batch);
			}

		} else if (order == Order.DEFAULT && type == Type.BatchAfterPostProcessing) {

			Array<HudElement> items = managerAfterPost.items;
			for (int i = 0; i < items.size; i++) {
				items.get(i).onRender(GameEvents.gameRenderer.batch);
			}
		}

		if (batch != null) {
			batch.setTransformMatrix(identity);
		}
	}

	public void addBeforePostProcessing (HudElement element) {
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
		GameEvents.gameRenderer.removeListener(this, RenderEventBeforePost, RenderOrderBeforePost);
		GameEvents.gameRenderer.removeListener(this, RenderEventAfterPost, RenderOrderAfterPost);
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
