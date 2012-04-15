package com.bitfire.uracer.game;

import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.game.data.GameData;
import com.bitfire.uracer.game.logic.GameLogic;
import com.bitfire.uracer.game.player.Car.Aspect;
import com.bitfire.uracer.game.player.CarModel;
import com.bitfire.uracer.game.rendering.Debug;
import com.bitfire.uracer.task.TaskManager;

public class Game implements Disposable {

	// config
	public GameplaySettings gameSettings = null;

	// logic
	private GameLogic gameLogic = null;

	// rendering
	private GameRenderer gameRenderer = null;

	public Game( String levelName, GameDifficulty difficulty, Aspect carAspect, CarModel carModel ) {
		GameData.create( URacer.getScalingStrategy(), levelName, false, difficulty, carAspect, carModel );

		gameRenderer = new GameRenderer( GameData.Environment.scalingStrategy, GameData.Environment.gameWorld );

		// handle game rules and mechanics, it's all about game data
		gameLogic = new GameLogic( gameRenderer.postProcessor );
	}

	@Override
	public void dispose() {
		gameLogic.dispose();
		gameRenderer.dispose();
		GameData.dispose();
	}

	public boolean tick() {
		TaskManager.dispatchTick();

		if( !gameLogic.onTick() ) {
			return false;
		}

		Debug.tick();
		return true;
	}

	public void render() {
		gameLogic.onBeforeRender();
		gameRenderer.render();
	}

	public void pause() {
	}

	public void resume() {
		gameRenderer.rebind();
	}
}