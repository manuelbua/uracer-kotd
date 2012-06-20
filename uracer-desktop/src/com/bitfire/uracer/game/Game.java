package com.bitfire.uracer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.actors.CarPreset;
import com.bitfire.uracer.game.logic.GameLogic;
import com.bitfire.uracer.game.logic.replaying.Replay;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.task.TaskManager;

public class Game implements Disposable {

	// world
	public GameWorld gameWorld = null;

	// debug
	private DebugHelper debug = null;

	// logic
	private GameLogic gameLogic = null;

	// rendering
	private GameRenderer gameRenderer = null;

	public Game( String levelName, ScalingStrategy scalingStrategy ) {

		gameWorld = new GameWorld( scalingStrategy, levelName, false );
		Gdx.app.log( "Game", "Game world ready" );

		// handles rendering
		gameRenderer = new GameRenderer( gameWorld, scalingStrategy );
		Gdx.app.log( "Game", "GameRenderer ready" );

		// handles game rules and mechanics, it's all about game data
		gameLogic = new GameLogic( gameWorld, gameRenderer, scalingStrategy );
		Gdx.app.log( "Game", "GameLogic created" );

		// initialize the debug helper
		if( Config.Debug.UseDebugHelper ) {
			debug = new DebugHelper( gameRenderer.getWorldRenderer(), gameWorld.getBox2DWorld(), gameRenderer.getPostProcessor() );
			Gdx.app.log( "Game", "Debug helper initialized" );
		}
	}

	@Override
	public void dispose() {
		if( Config.Debug.UseDebugHelper ) {
			debug.dispose();
		}

		TaskManager.dispose();
		gameRenderer.dispose();
		gameLogic.dispose();
	}

	public void setPlayer( CarPreset.Type presetType ) {
		gameLogic.setPlayer( presetType );
		if( Config.Debug.UseDebugHelper ) {
			DebugHelper.setPlayer( gameLogic.getPlayer() );
		}
	}

	public void setLocalReplay( Replay replay ) {
		gameLogic.setBestLocalReplay( replay );
	}

	public void tick() {
		TaskManager.dispatchTick();
		gameLogic.onAcquireInput();
	}

	public boolean tickCompleted() {
		gameLogic.onSubstepCompleted();

		if( gameLogic.doQuit ) {
			return true;
		}

		return false;
	}

	public void render() {
		// trigger the event and let's subscribers interpolate and update their state()
		gameRenderer.beforeRender( URacer.getTemporalAliasing() );
		gameLogic.onBeforeRender();


		gameRenderer.render();
	}

	public void debugUpdate() {
		debug.update();
		gameRenderer.debugRender();
	}

	public void pause() {
	}

	public void resume() {
		gameRenderer.rebind();
	}
}