package com.bitfire.uracer;

import com.bitfire.uracer.carsimulation.CarModel;
import com.bitfire.uracer.game.Game;
import com.bitfire.uracer.game.GameDifficulty;
import com.bitfire.uracer.game.actors.CarAspect;

public class GameScreen extends Screen {
	private Game game = null;
	private boolean quit = false;

	public GameScreen() {
		game = new Game( "tutorial-4", GameDifficulty.Hard, CarAspect.OldSkool, new CarModel().toModel2() );
	}

	@Override
	public void init() {
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
