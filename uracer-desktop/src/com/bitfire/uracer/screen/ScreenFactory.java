package com.bitfire.uracer.screen;

import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.screen.screens.GameScreen;
import com.bitfire.uracer.screen.screens.MainScreen;

public final class ScreenFactory {

	public enum ScreenType {
		//@formatter:off
		ExitScreen,
		MainScreen,
		ConfigurationScreen,
		GameScreen
		//@formatter:on
	}

	public static Screen createScreen( ScreenType screenType, ScalingStrategy strategy ) {
		Screen screen = null;

		switch( screenType ) {
		case GameScreen:
			screen = new GameScreen();
			break;
		case MainScreen:
			screen = new MainScreen();
			break;
		case ConfigurationScreen:
		default:
			screen = null;
			break;
		}

		if( screen != null ) {
			screen.init( strategy );
		}

		return screen;
	}

	private ScreenFactory() {
	}
}
