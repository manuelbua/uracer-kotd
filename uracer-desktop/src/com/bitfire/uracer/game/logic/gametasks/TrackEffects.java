
package com.bitfire.uracer.game.logic.gametasks;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.IntMap;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.events.GameRendererEvent;
import com.bitfire.uracer.game.events.GameRendererEvent.Order;
import com.bitfire.uracer.game.events.GameRendererEvent.Type;
import com.bitfire.uracer.game.logic.gametasks.trackeffects.TrackEffect;
import com.bitfire.uracer.game.logic.gametasks.trackeffects.TrackEffectType;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.utils.ItemsManager;

public final class TrackEffects extends GameTask implements DisposableTasks {
	private final ItemsManager<TrackEffect> managerBeforeCars = new ItemsManager<TrackEffect>();
	private final ItemsManager<TrackEffect> managerAfterCars = new ItemsManager<TrackEffect>();
	private final IntMap<TrackEffect> effectsMap = new IntMap<TrackEffect>();

	private final GameRendererEvent.Listener listener = new GameRendererEvent.Listener() {
		@Override
		public void handle (Object source, Type type, Order order) {
			if (order != GameRendererEvent.Order.DEFAULT) {
				return;
			}

			SpriteBatch batch = GameEvents.gameRenderer.batch;

			if (type == GameRendererEvent.Type.BatchBeforeCars) {
				// after entities
				for (TrackEffect e : managerBeforeCars) {
					e.render(batch);
				}
			} else if (type == GameRendererEvent.Type.BatchAfterCars) {
				for (TrackEffect e : managerAfterCars) {
					e.render(batch);
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
		disposeTasks();
	}

	@Override
	public void disposeTasks () {
		managerBeforeCars.dispose();
		managerAfterCars.dispose();
	}

	@Override
	public void onTick () {
		if (isPaused) return;

		for (TrackEffect e : managerBeforeCars) {
			e.tick();
		}

		for (TrackEffect e : managerAfterCars) {
			e.tick();
		}
	}

	@Override
	public void onGameRestart () {
		onGameReset();
	}

	@Override
	protected void onGamePause () {
		super.onGamePause();

		for (TrackEffect e : managerBeforeCars) {
			e.gamePause();
		}

		for (TrackEffect e : managerAfterCars) {
			e.gamePause();
		}
	}

	@Override
	protected void onGameResume () {
		super.onGameResume();

		for (TrackEffect e : managerBeforeCars) {
			e.gameResume();
		}

		for (TrackEffect e : managerAfterCars) {
			e.gameResume();
		}
	}

	@Override
	public void onGameReset () {
		for (TrackEffect e : managerBeforeCars) {
			e.reset();
		}

		for (TrackEffect e : managerAfterCars) {
			e.reset();
		}
	}

	public int getParticleCount () {
		int total = 0;

		for (TrackEffect e : managerBeforeCars) {
			total += e.getParticleCount();
		}

		for (TrackEffect e : managerAfterCars) {
			total += e.getParticleCount();
		}

		return total;
	}

	@Override
	public void onPlayer (PlayerCar player) {
		super.onPlayer(player);

		for (TrackEffect e : managerBeforeCars) {
			e.player(player);
		}

		for (TrackEffect e : managerAfterCars) {
			e.player(player);
		}
	}
}
