package com.bitfire.uracer;

import com.bitfire.uracer.game.Game;
import com.bitfire.uracer.game.GameDifficulty;
import com.bitfire.uracer.game.actors.Car.Aspect;
import com.bitfire.uracer.game.actors.CarModel;
import com.bitfire.uracer.game.logic.replaying.Replay;

public class GameScreen extends Screen {
	private Game game = null;
	private boolean quit = false;

	public GameScreen() {
	}

	@Override
	public void init( ScalingStrategy scalingStrategy ) {
		String levelName = "tutorial-3";

		game = new Game( levelName, scalingStrategy, GameDifficulty.Hard );

		// simulate the player choosing a car type
		game.setPlayer( new CarModel().toModel2(), Aspect.OldSkool2 );

		// simulate the player choosing local playing
		Replay r = Replay.loadLocal( levelName );
		if( r != null ) {
			game.setLocalReplay( r );
		}
	}

	@Override
	public void removed() {
		game.dispose();
		game = null;
	}

	@Override
	public void tick() {
		if( quit ) {
			return;
		}

		quit = !game.tick();
	}

	@Override
	public boolean quit() {
		return quit;
	}

	@Override
	public void render() {
		if( quit ) {
			return;
		}

		game.render();
	}

	@Override
	public void pause() {
		game.pause();
	}

	@Override
	public void resume() {
		game.resume();
	}

}
