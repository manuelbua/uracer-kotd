package com.bitfire.uracer.game.logic.sounds;

import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.game.logic.GameTask;
import com.bitfire.uracer.utils.Manager;

public class SoundManager extends GameTask {
	private final Manager<SoundEffect> manager = new Manager<SoundEffect>();

	public SoundManager() {
	}

	@Override
	public void dispose() {
		manager.dispose();
	}

	public void add( SoundEffect effect ) {
		manager.add( effect );
	}

	public void remove( SoundEffect effect ) {
		manager.remove( effect );
	}

	@Override
	protected void onTick() {
		Array<SoundEffect> items = manager.items;
		for( int i = 0; i < items.size; i++ ) {
			items.get( i ).tick();
		}
	}

	@Override
	public void onReset() {
		Array<SoundEffect> items = manager.items;
		for( int i = 0; i < items.size; i++ ) {
			items.get( i ).reset();
		}
	}
}
