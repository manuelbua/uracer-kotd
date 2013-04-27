
package com.bitfire.uracer.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.UserPreferences;
import com.bitfire.uracer.configuration.UserPreferences.Preference;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.game.Game;
import com.bitfire.uracer.game.GameLevels;
import com.bitfire.uracer.game.actors.CarPreset.Type;
import com.bitfire.uracer.game.screens.GameScreensFactory.ScreenType;
import com.bitfire.uracer.screen.Screen;

public class GameScreen extends Screen {
	private Game game = null;

	@Override
	public void init () {
		// simulate slowness
		// try { Thread.sleep( 1000 ); } catch( InterruptedException e ) {}

		if (!GameLevels.levelIdExists(ScreensShared.selectedLevelId)) {
			Gdx.app.error("GameScreen", "The specified track could not be found.");
			URacer.Game.show(ScreenType.MainScreen);
		} else {
			// save as last played track
			UserPreferences.string(Preference.LastPlayedTrack, ScreensShared.selectedLevelId);

			UserProfile userProfile = new UserProfile();
			game = new Game(userProfile, ScreensShared.selectedLevelId);

			// choose a car type
			game.setPlayer(Type.L2_PinkBeast);
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
	}

	@Override
	public void tickCompleted () {
		if (game != null) game.tickCompleted();
	}

	@Override
	public void render (FrameBuffer dest) {
		if (game != null) game.render(dest);
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
	public void debugRender () {
		if (game != null) game.debugUpdate();
	}
}
