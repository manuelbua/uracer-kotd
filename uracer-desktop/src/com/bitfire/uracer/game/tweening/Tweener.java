package com.bitfire.uracer.game.tweening;

import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenAccessor;
import aurelienribon.tweenengine.TweenManager;

import com.bitfire.uracer.game.GameData.Events;
import com.bitfire.uracer.game.events.GameLogicEvent;

public class Tweener {
	private TweenManager manager;

	private final GameLogicEvent.Listener gameLogicEvent = new GameLogicEvent.Listener() {
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

	public Tweener() {
		Events.gameLogic.addListener( gameLogicEvent );
		manager = new TweenManager();
	}

	public void dispose() {
		manager.killAll();
	}

	public static void registerAccessor( Class someClass, TweenAccessor accessor ) {
		Tween.registerAccessor( someClass, accessor );
	}

	public void clear() {
		manager.killAll();
	}

	public void start( Timeline timeline ) {
		timeline.start( manager );
	}

	public void update( int deltaMillis ) {
		manager.update( deltaMillis );
	}
}
