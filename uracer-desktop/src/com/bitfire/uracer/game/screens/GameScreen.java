
package com.bitfire.uracer.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.bitfire.uracer.Input;
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
	private Input input = null;
	private boolean paused = false;
	private GameScreenUI gameui;

	@Override
	public void init () {
		if (!GameLevels.levelIdExists(ScreensShared.selectedLevelId)) {
			Gdx.app.error("GameScreen", "The specified track could not be found.");
			URacer.Game.show(ScreenType.MainScreen);
		} else {
			input = URacer.Game.getInputSystem();

			// save as last played track
			UserPreferences.string(Preference.LastPlayedTrack, ScreensShared.selectedLevelId);
			UserProfile userProfile = new UserProfile();
			game = new Game(userProfile, ScreensShared.selectedLevelId);

			// build in-game UI
			gameui = new GameScreenUI(game);

			game.start();
		}
	}

	@Override
	public void dispose () {
		if (game != null) game.dispose();
		game = null;
	}

	@Override
	public void tick () {
		if (game != null) game.tick();
		if (paused) gameui.tick();

		// toggle in-game menu, this shortcut shall be always available
		if (input.isPressed(Keys.ESCAPE)) {
			paused = !paused;
			if (paused) {
				game.pause();
				gameui.enable();
			} else {
				game.resume();
				gameui.disable();
			}
		}

		// quit shortcut
		// if (paused && input.isPressed(Keys.Q)) {
		// game.quit();
		// }
	}

	@Override
	public void tickCompleted () {
		if (game != null) game.tickCompleted();
	}

	@Override
	public void pause () {
		if (game != null) game.pause();
	}

	@Override
	public void resume () {
		if (game != null) game.resume();
	}

	@Override
	public void render (FrameBuffer dest) {
		if (game != null) game.render(dest);

		// overlay the whole in-game UI
		if (paused) {
			gameui.render(dest);
		}
	}
}
