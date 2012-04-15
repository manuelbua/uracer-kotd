package com.bitfire.uracer.game.logic.sounds;

import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.game.events.GameEvents;
import com.bitfire.uracer.game.events.GameLogicEvent;
import com.bitfire.uracer.game.events.GameLogicEvent.Type;
import com.bitfire.uracer.task.Task;
import com.bitfire.uracer.utils.Manager;

public class SoundManager extends Task {
	private final Manager<ISoundEffect> manager = new Manager<ISoundEffect>();

	private final GameLogicEvent.Listener gameLogicEvent = new GameLogicEvent.Listener() {
		@Override
		public void gameLogicEvent( Type type ) {
			switch( type ) {
			case onRestart:
			case onReset:
				reset();
				break;
			}
		}
	};

	public SoundManager() {
		GameEvents.gameLogic.addListener( gameLogicEvent, GameLogicEvent.Type.onReset );
		GameEvents.gameLogic.addListener( gameLogicEvent, GameLogicEvent.Type.onRestart );
	}

	@Override
	public void dispose() {
		manager.dispose();
	}

	public void add( ISoundEffect effect ) {
		manager.add( effect );
	}

	public void remove( ISoundEffect effect ) {
		manager.remove( effect );
	}

	@Override
	protected void onTick() {
		Array<ISoundEffect> items = manager.items;
		for( int i = 0; i < items.size; i++ ) {
			items.get( i ).tick();
		}
	}

	public void reset() {
		Array<ISoundEffect> items = manager.items;
		for( int i = 0; i < items.size; i++ ) {
			items.get( i ).reset();
		}
	}
}
