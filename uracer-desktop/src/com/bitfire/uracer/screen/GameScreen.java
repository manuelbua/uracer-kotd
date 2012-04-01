package com.bitfire.uracer.screen;

import com.bitfire.uracer.Config;
import com.bitfire.uracer.game.Game;

public class GameScreen extends Screen {
	private Game game = null;
	private boolean quit = false;

	public GameScreen() {
		game = new Game( "tutorial-3", Config.Game.difficulty );
	}

	@Override
	public void removed() {
		super.removed();
		game.dispose();
		game = null;
	}

	@Override
	public void tick() {
		if( quit )
			return;
		quit = !game.tick();
	}

	@Override
	public boolean quit() {
		return quit;
	}

	@Override
	public void render() {
		if( quit )
			return;
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
