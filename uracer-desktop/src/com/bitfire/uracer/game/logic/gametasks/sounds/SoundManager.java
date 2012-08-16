
package com.bitfire.uracer.game.logic.gametasks.sounds;

import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.game.logic.gametasks.GameTask;
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
		manager.remove(effect);
	}

	@Override
	protected void onTick () {
		Array<SoundEffect> items = manager.items;
		for (int i = 0; i < items.size; i++) {
			items.get(i).tick();
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
