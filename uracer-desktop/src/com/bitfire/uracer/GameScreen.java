package com.bitfire.uracer;

import com.bitfire.uracer.game.Game;
import com.bitfire.uracer.game.GameDifficulty;
import com.bitfire.uracer.game.actors.CarPreset.Type;
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
		game.setPlayer( Type.FordMustangShelbyGt500White );

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
		game.tick();
	}

	@Override
	public void tickCompleted() {
		quit = game.tickCompleted();
	}

	@Override
	public void render() {
		game.render();
	}

	@Override
	public boolean quit() {
		return quit;
	}

	@Override
	public void pause() {
		game.pause();
	}

	@Override
	public void resume() {
		game.resume();
	}

	@Override
	public void debugUpdate() {
		game.debugUpdate();
	}
}
