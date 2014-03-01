
package com.bitfire.uracer.game.logic.gametasks;

import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.events.GameLogicEvent;
import com.bitfire.uracer.game.events.GameLogicEvent.Order;
import com.bitfire.uracer.game.events.GameLogicEvent.Type;
import com.bitfire.uracer.game.events.TaskManagerEvent;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.task.Task;
import com.bitfire.uracer.utils.URacerRuntimeException;

public abstract class GameTask extends Task implements Disposable {
	protected PlayerCar player;
	protected boolean hasPlayer = false;

	private GameLogicEvent.Listener logicListener = new GameLogicEvent.Listener() {
		@Override
		public void handle (Object source, Type type, Order order) {
			switch (type) {
			case PlayerAdded:
				onPlayer(GameEvents.logicEvent.player);
				break;
			case PlayerRemoved:
				if (GameEvents.logicEvent.player != null) {
					throw new URacerRuntimeException("Player zombieing");
				}

				onPlayer(null);
				break;
			case GameRestart:
				onGameRestart();
				break;
			case GameReset:
				onGameReset();
				break;
			case GameQuit:
				onGameQuit();
				break;
			}
		}
	};

	public GameTask () {
		this(TaskManagerEvent.Order.DEFAULT);
	}

	public GameTask (TaskManagerEvent.Order order) {
		super(order);
		GameEvents.logicEvent.addListener(logicListener, GameLogicEvent.Type.GameRestart);
		GameEvents.logicEvent.addListener(logicListener, GameLogicEvent.Type.GameReset);
		GameEvents.logicEvent.addListener(logicListener, GameLogicEvent.Type.GameQuit);
		GameEvents.logicEvent.addListener(logicListener, GameLogicEvent.Type.PlayerAdded);
		GameEvents.logicEvent.addListener(logicListener, GameLogicEvent.Type.PlayerRemoved);
	}

	@Override
	public void dispose () {
		super.dispose();
		GameEvents.logicEvent.removeListener(logicListener, GameLogicEvent.Type.GameRestart);
		GameEvents.logicEvent.removeListener(logicListener, GameLogicEvent.Type.GameReset);
		GameEvents.logicEvent.removeListener(logicListener, GameLogicEvent.Type.GameQuit);
		GameEvents.logicEvent.removeListener(logicListener, GameLogicEvent.Type.PlayerAdded);
		GameEvents.logicEvent.removeListener(logicListener, GameLogicEvent.Type.PlayerRemoved);
	}

	public void onPlayer (PlayerCar player) {
		this.player = player;
		this.hasPlayer = (player != null);
	}

	public void onGameReset () {
	}

	public void onGameRestart () {
	}

	public void onGameQuit () {
	}
}
