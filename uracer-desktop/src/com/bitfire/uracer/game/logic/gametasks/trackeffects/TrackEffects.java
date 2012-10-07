
package com.bitfire.uracer.game.logic.gametasks.trackeffects;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.events.GameRendererEvent;
import com.bitfire.uracer.events.GameRendererEvent.Order;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.logic.gametasks.GameTask;
import com.bitfire.utils.ItemsManager;

public final class TrackEffects extends GameTask {
	private static final GameRendererEvent.Type RenderEvent = GameRendererEvent.Type.BatchBeforeMeshes;

	private ItemsManager<TrackEffect> managerBeforeEntities = new ItemsManager<TrackEffect>();
	private ItemsManager<TrackEffect> managerAfterEntities = new ItemsManager<TrackEffect>();

	private final GameRendererEvent.Listener listener = new GameRendererEvent.Listener() {
		@Override
		public void gameRendererEvent (GameRendererEvent.Type type, Order order) {
			SpriteBatch batch = GameEvents.gameRenderer.batch;

			Array<TrackEffect> items = managerBeforeEntities.items;
			if (order == GameRendererEvent.Order.MINUS_4) {
				// before entities
			} else if (order == GameRendererEvent.Order.PLUS_4) {
				// after entities
				items = managerAfterEntities.items;
			}

			for (int i = 0; i < items.size; i++) {
				TrackEffect effect = items.get(i);
				if (effect != null) {
					effect.render(batch);
				}
			}
		}
	};

	public TrackEffects () {
		GameEvents.gameRenderer.addListener(listener, RenderEvent, GameRendererEvent.Order.MINUS_4);
		GameEvents.gameRenderer.addListener(listener, RenderEvent, GameRendererEvent.Order.PLUS_4);

		// NOTE for custom render event
		// for CarSkidMarks GameRenderer.event.addListener( gameRendererEvent, GameRendererEvent.Type.BatchBeforeMeshes,
		// GameRendererEvent.Order.Order_Minus_4 );
		// for SmokeTrails GameRenderer.event.addListener( gameRendererEvent, GameRendererEvent.Type.BatchBeforeMeshes,
		// GameRendererEvent.Order.Order_Minus_3 );
	}

	public void addBeforeEntities (TrackEffect effect) {
		managerBeforeEntities.add(effect);
	}

	public void addAfterEntities (TrackEffect effect) {
		managerAfterEntities.add(effect);
	}

	public void remove (TrackEffect effect) {
		managerBeforeEntities.remove(effect);
		managerAfterEntities.remove(effect);
	}

	@Override
	public void dispose () {
		super.dispose();
		GameEvents.gameRenderer.removeListener(listener, RenderEvent, GameRendererEvent.Order.MINUS_4);
		GameEvents.gameRenderer.removeListener(listener, RenderEvent, GameRendererEvent.Order.PLUS_4);

		Array<TrackEffect> items = managerBeforeEntities.items;
		for (int i = 0; i < items.size; i++) {
			items.get(i).dispose();
		}

		items = managerAfterEntities.items;
		for (int i = 0; i < items.size; i++) {
			items.get(i).dispose();
		}

		managerBeforeEntities.dispose();
		managerAfterEntities.dispose();
	}

	@Override
	public void onTick () {
		Array<TrackEffect> items = managerBeforeEntities.items;
		for (int i = 0; i < items.size; i++) {
			TrackEffect effect = items.get(i);
			effect.tick();
		}

		items = managerAfterEntities.items;
		for (int i = 0; i < items.size; i++) {
			TrackEffect effect = items.get(i);
			effect.tick();
		}

	}

	@Override
	public void onReset () {
		Array<TrackEffect> items = managerBeforeEntities.items;
		for (int i = 0; i < items.size; i++) {
			TrackEffect effect = items.get(i);
			effect.reset();
		}

		items = managerAfterEntities.items;
		for (int i = 0; i < items.size; i++) {
			TrackEffect effect = items.get(i);
			effect.reset();
		}

	}

	public int getParticleCount () {
		int total = 0;

		Array<TrackEffect> items = managerBeforeEntities.items;
		for (int i = 0; i < items.size; i++) {
			TrackEffect effect = items.get(i);
			total += effect.getParticleCount();
		}

		items = managerAfterEntities.items;
		for (int i = 0; i < items.size; i++) {
			TrackEffect effect = items.get(i);
			total += effect.getParticleCount();
		}

		return total;
	}
}
