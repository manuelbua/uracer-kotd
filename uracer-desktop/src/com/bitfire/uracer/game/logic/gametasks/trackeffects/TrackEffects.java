
package com.bitfire.uracer.game.logic.gametasks.trackeffects;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.bitfire.uracer.events.GameRendererEvent;
import com.bitfire.uracer.events.GameRendererEvent.Order;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.logic.gametasks.GameTask;
import com.bitfire.utils.ItemsManager;

public final class TrackEffects extends GameTask {
	private final ItemsManager<TrackEffect> managerBeforeCars = new ItemsManager<TrackEffect>();
	private final ItemsManager<TrackEffect> managerAfterCars = new ItemsManager<TrackEffect>();
	private final IntMap<TrackEffect> effectsMap = new IntMap<TrackEffect>();

	private final GameRendererEvent.Listener listener = new GameRendererEvent.Listener() {
		@Override
		public void gameRendererEvent (GameRendererEvent.Type type, Order order) {
			if (order != GameRendererEvent.Order.DEFAULT) {
				return;
			}

			SpriteBatch batch = GameEvents.gameRenderer.batch;

			Array<TrackEffect> items = null;

			if (type == GameRendererEvent.Type.BatchBeforeCars) {
				// after entities
				items = managerBeforeCars.items;
			} else if (type == GameRendererEvent.Type.BatchAfterCars) {
				items = managerAfterCars.items;
			}

			if (items != null) {
				for (int i = 0; i < items.size; i++) {
					TrackEffect effect = items.get(i);
					if (effect != null) {
						effect.render(batch);
					}
				}
			}
		}
	};

	public TrackEffects () {
		GameEvents.gameRenderer.addListener(listener, GameRendererEvent.Type.BatchBeforeCars, GameRendererEvent.Order.DEFAULT);
		GameEvents.gameRenderer.addListener(listener, GameRendererEvent.Type.BatchAfterCars, GameRendererEvent.Order.DEFAULT);
	}

	public void addBeforeCars (TrackEffect effect) {
		managerBeforeCars.add(effect);
		addToMap(effect);
	}

	public void addAfterCars (TrackEffect effect) {
		managerAfterCars.add(effect);
		addToMap(effect);
	}

	private void addToMap (TrackEffect effect) {
		if (!effectsMap.containsKey(effect.type.id)) {
			effectsMap.put(effect.type.id, effect);
		}
	}

	public TrackEffect getEffect (TrackEffectType type) {
		if (effectsMap.containsKey(type.id)) {
			return effectsMap.get(type.id);
		}

		return null;
	}

	public void remove (TrackEffect effect) {
		managerBeforeCars.remove(effect);
		managerAfterCars.remove(effect);
		effectsMap.remove(effect.type.id);
	}

	@Override
	public void dispose () {
		super.dispose();
		GameEvents.gameRenderer.removeListener(listener, GameRendererEvent.Type.BatchBeforeCars, GameRendererEvent.Order.DEFAULT);
		GameEvents.gameRenderer.removeListener(listener, GameRendererEvent.Type.BatchAfterCars, GameRendererEvent.Order.DEFAULT);

		Array<TrackEffect> items = managerBeforeCars.items;
		for (int i = 0; i < items.size; i++) {
			items.get(i).dispose();
		}

		items = managerAfterCars.items;
		for (int i = 0; i < items.size; i++) {
			items.get(i).dispose();
		}

		managerBeforeCars.dispose();
		managerAfterCars.dispose();
	}

	@Override
	public void onTick () {
		Array<TrackEffect> items = managerBeforeCars.items;
		for (int i = 0; i < items.size; i++) {
			TrackEffect effect = items.get(i);
			effect.tick();
		}

		items = managerAfterCars.items;
		for (int i = 0; i < items.size; i++) {
			TrackEffect effect = items.get(i);
			effect.tick();
		}

	}

	@Override
	public void onReset () {
		Array<TrackEffect> items = managerBeforeCars.items;
		for (int i = 0; i < items.size; i++) {
			TrackEffect effect = items.get(i);
			effect.reset();
		}

		items = managerAfterCars.items;
		for (int i = 0; i < items.size; i++) {
			TrackEffect effect = items.get(i);
			effect.reset();
		}

	}

	public int getParticleCount () {
		int total = 0;

		Array<TrackEffect> items = managerBeforeCars.items;
		for (int i = 0; i < items.size; i++) {
			TrackEffect effect = items.get(i);
			total += effect.getParticleCount();
		}

		items = managerAfterCars.items;
		for (int i = 0; i < items.size; i++) {
			TrackEffect effect = items.get(i);
			total += effect.getParticleCount();
		}

		return total;
	}
}
