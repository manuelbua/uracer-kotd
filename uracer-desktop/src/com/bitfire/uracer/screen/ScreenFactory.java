package com.bitfire.uracer.screen;

import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.screen.screens.GameScreen;
import com.bitfire.uracer.screen.screens.MainScreen;

public final class ScreenFactory {

	public enum ScreenType {
		//@formatter:off
		NoScreen,
		ExitScreen,
		MainScreen,
		ConfigurationScreen,
		GameScreen
		//@formatter:on
	}

	public static Screen createScreen( ScreenType screenType ) {
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

	public static void init( ScalingStrategy strategy ) {
		ScreenFactory.strategy = strategy;
	}

	private static ScalingStrategy strategy;

	private ScreenFactory() {
	}
}
