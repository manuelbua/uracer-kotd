package com.bitfire.uracer.screen;

import com.bitfire.uracer.ScalingStrategy;

public final class ScreenFactory {

	public enum ScreenType {
		ExitScreen, GameScreen
	}

	public static Screen createScreen( ScreenType screenType, ScalingStrategy strategy ) {
		Screen screen = null;

		switch( screenType ) {
		case GameScreen:
			screen = new GameScreen();
			break;
		default:
		case ExitScreen:
			screen = null;
			break;
		}

		if( screen != null ) {
			screen.init( strategy );
		}

		return screen;
	}
}
