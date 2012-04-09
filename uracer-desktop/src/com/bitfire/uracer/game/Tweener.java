package com.bitfire.uracer.game;

import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.TweenManager;

import com.bitfire.uracer.game.events.GameLogicEvent;

public final class Tweener {
	private static final TweenManager manager = new TweenManager();

	private static final GameLogicEvent.Listener gameLogicEvent = new GameLogicEvent.Listener() {
		@Override
		public void gameLogicEvent( GameLogicEvent.Type type ) {
			switch( type ) {
			case onReset:
			case onRestart:
				clear();
				break;
			}
		}
	};

	public static void init() {
		GameEvents.gameLogic.addListener( gameLogicEvent );
	}

	private Tweener() {
	}

	public static void dispose() {
		clear();
	}

	public static void clear() {
		manager.killAll();
	}

	public static void start( Timeline timeline ) {
		timeline.start( manager );
	}

	public static void update( int deltaMillis ) {
		manager.update( deltaMillis );
	}
}
