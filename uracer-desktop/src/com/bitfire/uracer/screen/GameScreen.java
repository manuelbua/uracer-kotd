package com.bitfire.uracer.screen;

import com.bitfire.uracer.Screen;
import com.bitfire.uracer.carsimulation.CarModel;
import com.bitfire.uracer.game.Game;
import com.bitfire.uracer.game.GameDifficulty;
import com.bitfire.uracer.game.actors.Car.CarType;

public class GameScreen extends Screen {
	private Game game = null;
	private boolean quit = false;

	public GameScreen() {
		game = new Game( "tutorial-3", GameDifficulty.Hard, CarType.OldSkool, new CarModel().toModel2() );
	}

	@Override
	public void removed() {
		super.removed();
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
