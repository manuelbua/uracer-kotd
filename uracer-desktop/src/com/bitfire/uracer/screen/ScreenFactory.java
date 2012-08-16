
package com.bitfire.uracer.screen;

import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.screen.screens.GameScreen;
import com.bitfire.uracer.screen.screens.MainScreen;
import com.bitfire.uracer.screen.screens.OptionsScreen;

public final class ScreenFactory {

	public enum ScreenType {
		// @off
		NoScreen, ExitScreen, MainScreen, OptionsScreen, GameScreen
		// @on
	}

	public static Screen createScreen (ScreenType screenType) {
		Screen screen = null;

		switch (screenType) {
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
			screen.init(strategy);
		}

		return screen;
	}

	public static void init (ScalingStrategy strategy) {
		ScreenFactory.strategy = strategy;
	}

	private static ScalingStrategy strategy;

	private ScreenFactory () {
	}
}
