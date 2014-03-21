
package com.bitfire.uracer.game.screens;

import com.bitfire.uracer.screen.Screen;
import com.bitfire.uracer.screen.ScreenFactory;

public final class GameScreensFactory implements ScreenFactory {

	public enum ScreenType implements ScreenId {
		// @off
		NoScreen, ExitScreen, MainScreen, OptionsScreen, GameScreen
		;
		// @on

		@Override
		public int id () {
			return this.ordinal();
		}
	}

	@Override
	public Screen createScreen (ScreenId screenId) {
		Screen screen = null;

		switch (types[screenId.id()]) {
		case GameScreen:
			screen = new GameScreen();
			break;
		case MainScreen:
			screen = new MainScreen();
			break;
		case OptionsScreen:
			screen = new OptionsScreen();
			break;
		case ExitScreen:
		default:
			screen = null;
			break;
		}

		if (screen != null) {
			if (screen.init()) {
				if (screenId == ScreenType.GameScreen) {
					screen.tick();
					screen.tickCompleted();
				}
			}
		}

		return screen;
	}

	private ScreenType[] types = ScreenType.values();
}
