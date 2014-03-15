
package com.bitfire.uracer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.configuration.UserPreferences;
import com.bitfire.uracer.configuration.UserPreferences.Preference;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.game.events.TaskManagerEvent;
import com.bitfire.uracer.game.logic.SinglePlayer;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.task.TaskManager;
import com.bitfire.uracer.game.world.GameWorld;

public class Game implements Disposable {

	// world
	public GameWorld gameWorld = null;

	// logic
	private GameLogic gameLogic = null;

	// tasks
	private TaskManager taskManager = null;

	// rendering
	private GameRenderer gameRenderer = null;

	public Game (UserProfile userProfile, String trackId) {

		taskManager = new TaskManager();

		gameWorld = new GameWorld(trackId, UserPreferences.bool(Preference.NightMode));
		Gdx.app.debug("Game", "Game world ready");

		// handles rendering
		gameRenderer = new GameRenderer(gameWorld);
		Gdx.app.debug("Game", "GameRenderer ready");

		// handles game rules and mechanics, it's all about game data
		gameLogic = new SinglePlayer(userProfile, gameWorld, gameRenderer);
		Gdx.app.debug("Game", "GameLogic created");
	}

	@Override
	public void dispose () {
		gameLogic.dispose();
		gameRenderer.dispose();
		gameWorld.dispose();
		taskManager.dispose();
	}

	/** Can be NOT called */
	public void tick () {
		if (!gameLogic.isQuitPending()) {
			taskManager.dispatchEvent(TaskManagerEvent.Type.onTick);
		}

		gameLogic.tick();
	}

	/** Can be NOT called */
	public void tickCompleted () {
		if (!gameLogic.isQuitPending()) {
			taskManager.dispatchEvent(TaskManagerEvent.Type.onTickCompleted);
		}

		gameLogic.tickCompleted();
	}

	public void render (FrameBuffer dest) {
		gameRenderer.render(dest, gameLogic.isQuitPending(), gameLogic.isPaused());
	}

	public void pause () {
		taskManager.dispatchEvent(TaskManagerEvent.Type.onPause);
		gameLogic.pauseGame();
		Gdx.app.log("Game", "Paused");
	}

	public void resume () {
		gameRenderer.rebind();
		taskManager.dispatchEvent(TaskManagerEvent.Type.onResume);
		gameLogic.resumeGame();
		Gdx.app.log("Game", "Resumed");
	}

	public boolean isPaused () {
		return gameLogic.isPaused();
	}

	//
	// OPERATIONS
	//

	public void start () {
		gameLogic.addPlayer();
		gameLogic.restartGame();
	}

	public void quit () {
		gameLogic.quitGame();
	}
}
