package com.bitfire.uracer.screen;

import com.bitfire.uracer.ScalingStrategy;

public final class ScreenFactory {

	public enum ScreenType {
		GameScreen
	}

	public static Screen createScreen( ScreenType screen, ScalingStrategy strategy ) {
		Screen s = null;

		switch( screen ) {
		case GameScreen:
			s = new GameScreen();
			break;
		}

		if( s != null ) {
			s.init( strategy );
		}

		return s;
	}
}
