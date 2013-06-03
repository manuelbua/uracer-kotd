
package com.bitfire.uracer.game.logic.gametasks;

import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.game.logic.gametasks.sounds.SoundEffect;
import com.bitfire.utils.ItemsManager;

public class SoundManager extends GameTask {
	private final ItemsManager<SoundEffect> manager = new ItemsManager<SoundEffect>();

	public SoundManager () {
	}

	@Override
	public void dispose () {
		super.dispose();

		Array<SoundEffect> items = manager.items;
		for (int i = 0; i < items.size; i++) {
			items.get(i).dispose();
		}

		manager.dispose();
	}

	public void add (SoundEffect effect) {
		manager.add(effect);
	}

	public void remove (SoundEffect effect) {
		effect.stop();
		manager.remove(effect);
	}

	public void stop () {
		Array<SoundEffect> items = manager.items;
		for (int i = 0; i < items.size; i++) {
			items.get(i).stop();
		}
	}

	@Override
	public void onPause () {
		Array<SoundEffect> items = manager.items;
		for (int i = 0; i < items.size; i++) {
			items.get(i).pause();
		}
	}

	@Override
	public void onResume () {
		Array<SoundEffect> items = manager.items;
		for (int i = 0; i < items.size; i++) {
			items.get(i).resume();
		}
	}

	@Override
	protected void onTick () {
		Array<SoundEffect> items = manager.items;
		for (int i = 0; i < items.size; i++) {
			items.get(i).tick();
		}
	}

	@Override
	public void onRestart () {
		Array<SoundEffect> items = manager.items;
		for (int i = 0; i < items.size; i++) {
			items.get(i).restart();
		}
	}

	@Override
	public void onReset () {
		Array<SoundEffect> items = manager.items;
		for (int i = 0; i < items.size; i++) {
			items.get(i).reset();
		}
	}
}
