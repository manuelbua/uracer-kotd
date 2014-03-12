
package com.bitfire.uracer.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.UserPreferences;
import com.bitfire.uracer.configuration.UserPreferences.Preference;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.game.Game;
import com.bitfire.uracer.game.GameLevels;
import com.bitfire.uracer.game.screens.GameScreensFactory.ScreenType;
import com.bitfire.uracer.screen.Screen;

public class GameScreen extends Screen {
	private Game game = null;
	// private Input input = null;
	private GameScreenUI gameui;

	@Override
	public void init () {
		if (GameLevels.levelIdExists(ScreensShared.selectedLevelId)) {
			// input = URacer.Game.getInputSystem();

			// save as last played track
			UserPreferences.string(Preference.LastPlayedTrack, ScreensShared.selectedLevelId);
			UserProfile userProfile = new UserProfile();
			game = new Game(userProfile, ScreensShared.selectedLevelId);

			// build in-game UI
			gameui = new GameScreenUI(game);

			game.start();
		} else {
			Gdx.app.error("GameScreen", "The specified track could not be found.");
			URacer.Game.show(ScreenType.MainScreen);
		}
	}

	@Override
	public void dispose () {
		game.dispose();
		gameui.dispose();
		game = null;
	}

	@Override
	public void tick () {
		gameui.tick();
		game.tick();
	}

	@Override
	public void tickCompleted () {
		game.tickCompleted();
	}

	@Override
	public void pause () {
		game.pause();
	}

	@Override
	public void resume () {
		game.resume();
	}

	@Override
	public void render (FrameBuffer dest) {
		game.render(dest);

		// overlay the whole in-game UI
		gameui.render(dest);
	}
}
