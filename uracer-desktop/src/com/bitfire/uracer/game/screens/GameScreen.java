
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
	private GameScreenUI gameui;
	private boolean initialized = false;

	@Override
	public boolean init () {
		if (GameLevels.levelIdExists(ScreensShared.selectedLevelId)) {

			// save as last played track
			UserPreferences.string(Preference.LastPlayedTrack, ScreensShared.selectedLevelId);
			UserProfile userProfile = new UserProfile();
			game = new Game(userProfile, ScreensShared.selectedLevelId);

			// build in-game UI
			gameui = new GameScreenUI(game);

			game.start();
			initialized = true;
			return true;
		} else {
			// last saved level doesn't exists, so reset it
			ScreensShared.selectedLevelId = "";
			UserPreferences.string(Preference.LastPlayedTrack, "");
			UserPreferences.save();

			Gdx.app.error("GameScreen", "The specified track could not be found.");

			URacer.Game.show(ScreenType.MainScreen);
			initialized = false;
			return false;
		}
	}

	@Override
	public void dispose () {
		if (!initialized) return;

		game.dispose();
		gameui.dispose();

		// FIXME is this still needed to hint the VM at it?
		game = null;
		gameui = null;
	}

	@Override
	public void tick () {
		if (!initialized) return;

		gameui.tick();
		if (!game.isPaused()) game.tick();
	}

	@Override
	public void tickCompleted () {
		if (!initialized) return;

		game.tickCompleted();
	}

	@Override
	public void pause () {
		if (!initialized) return;

		game.pause();
	}

	@Override
	public void resume () {
		if (!initialized) return;

		game.resume();
	}

	@Override
	public void render (FrameBuffer dest) {
		if (!initialized) return;

		game.render(dest);

		// overlay the whole in-game UI
		gameui.render(dest);
	}
}
