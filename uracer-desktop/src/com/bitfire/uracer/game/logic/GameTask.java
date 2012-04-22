package com.bitfire.uracer.game.logic;

import com.bitfire.uracer.game.events.GameEvents;
import com.bitfire.uracer.game.events.GameLogicEvent;
import com.bitfire.uracer.game.events.GameLogicEvent.Type;
import com.bitfire.uracer.task.Task;

public abstract class GameTask extends Task {

	public GameTask( GameLogic logic ) {
		GameEvents.gameLogic.addListener( gameLogicEvent, GameLogicEvent.Type.onReset );
		GameEvents.gameLogic.addListener( gameLogicEvent, GameLogicEvent.Type.onRestart );
	}

	public abstract void onReset();

	public void onRestart() {
		onReset();
	}

	private final GameLogicEvent.Listener gameLogicEvent = new GameLogicEvent.Listener() {
		@Override
		public void gameLogicEvent( Type type ) {
			switch( type ) {
			case onRestart:
				onRestart();
				break;

			case onReset:
				onReset();
				break;
			}
		}
	};
}
