
package com.bitfire.uracer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.configuration.UserPreferences;
import com.bitfire.uracer.configuration.UserPreferences.Preference;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.events.TaskManagerEvent;
import com.bitfire.uracer.game.logic.types.SinglePlayer;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.task.TaskManager;
import com.bitfire.uracer.game.world.GameWorld;

public class Game implements Disposable {

	// world
	public GameWorld gameWorld = null;

	// debug
	private DebugHelper debug = null;

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

		// initialize the debug helper
		if (Config.Debug.UseDebugHelper) {
			debug = new DebugHelper(gameRenderer.getWorldRenderer(), gameWorld.getBox2DWorld(), gameRenderer.getPostProcessing()
				.getPostProcessor());
			Gdx.app.debug("Game", "Debug helper initialized");
		}
	}

	@Override
	public void dispose () {
		if (Config.Debug.UseDebugHelper) {
			debug.dispose();
		}

		gameLogic.dispose();
		gameRenderer.dispose();
		gameWorld.dispose();

		taskManager.dispose();
	}

	/** Can be NOT called */
	public void tick () {
		taskManager.dispatchEvent(TaskManagerEvent.Type.onTick);
		gameLogic.tick();
	}

	/** Can be NOT called */
	public void tickCompleted () {
		taskManager.dispatchEvent(TaskManagerEvent.Type.onTickCompleted);
		gameLogic.tickCompleted();
	}

	public void render (FrameBuffer dest) {
		// the order is important: first trigger interpolables to update their position and orientation, then give a
		// chance to use this information to the game logic
		gameRenderer.interpolate(URacer.Game.getTemporalAliasing());

		// updates based on interpolables
		gameLogic.beforeRender();

		// consume updated data
		gameRenderer.beforeRender();
		gameRenderer.render(dest);
	}

	public void debugUpdate () {
		if (Config.Debug.UseDebugHelper) {
			debug.update();
		}

		gameRenderer.debugRender();
	}

	public void pause () {
		taskManager.dispatchEvent(TaskManagerEvent.Type.onPause);
	}

	public void resume () {
		gameRenderer.rebind();
		taskManager.dispatchEvent(TaskManagerEvent.Type.onResume);
	}

	//
	// OPERATIONS
	//

	public void addPlayer () {
		gameLogic.addPlayer();
	}

}
