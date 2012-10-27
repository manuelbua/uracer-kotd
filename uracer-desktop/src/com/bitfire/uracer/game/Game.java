
package com.bitfire.uracer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.game.actors.CarPreset;
import com.bitfire.uracer.game.logic.types.CommonLogic;
import com.bitfire.uracer.game.logic.types.SinglePlayerLogic;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.task.TaskManager;
import com.bitfire.uracer.game.world.GameWorld;

public class Game implements Disposable {

	// world
	public GameWorld gameWorld = null;

	// debug
	private DebugHelper debug = null;

	// logic
	private CommonLogic gameLogic = null;

	// rendering
	private GameRenderer gameRenderer = null;

	public Game (UserProfile userProfile, String trackId, ScalingStrategy scalingStrategy) {

		gameWorld = new GameWorld(scalingStrategy, trackId, false);
		Gdx.app.debug("Game", "Game world ready");

		// handles rendering
		gameRenderer = new GameRenderer(gameWorld, scalingStrategy);
		Gdx.app.debug("Game", "GameRenderer ready");

		// handles game rules and mechanics, it's all about game data
		gameLogic = new SinglePlayerLogic(userProfile, gameWorld, gameRenderer, scalingStrategy);
		Gdx.app.debug("Game", "GameLogic created");

		// initialize the debug helper
		if (Config.Debug.UseDebugHelper) {
			debug = new DebugHelper(gameRenderer.getWorldRenderer(), gameWorld.getBox2DWorld(), gameRenderer.getPostProcessor());
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

		TaskManager.dispose();
	}

// public void setLocalReplay (Replay replay) {
// gameLogic.setBestLocalReplay(replay);
// }

	public void tick () {
		TaskManager.dispatchTick();
		gameLogic.tick();
	}

	public void tickCompleted () {
		gameLogic.tickCompleted();
	}

	public void render (FrameBuffer dest) {
		// the order is important: first trigger interpolable to update their
		// position and orientation, then give a chance to use this information
		// to the game logic
		gameRenderer.beforeRender(URacer.Game.getTemporalAliasing());
		gameLogic.beforeRender();

		gameRenderer.render(dest);
	}

	public void debugUpdate () {
		debug.update();
		gameRenderer.debugRender();
	}

	public void pause () {
	}

	public void resume () {
		gameRenderer.rebind();
	}

	//
	// OPERATIONS
	//

	public void setPlayer (CarPreset.Type presetType) {
		gameLogic.setPlayer(presetType);
	}

}
