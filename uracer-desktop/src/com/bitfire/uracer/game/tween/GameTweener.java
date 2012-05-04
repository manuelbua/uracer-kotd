package com.bitfire.uracer.game.tween;

import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.TweenManager;

import com.bitfire.uracer.URacer;

/** This tweener will update taking the timeMultiplier modulation into account */
public final class GameTweener {
	private static final TweenManager manager = new TweenManager();

	private GameTweener() {
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

	public static void update() {
		// TODO, check this, the documentation doesn't state it, looks like milliseconds
//		manager.update( 1000 * URacer.getLastDeltaSecs() * URacer.timeMultiplier);
		manager.update( URacer.getLastDeltaMs() * URacer.timeMultiplier);
	}
}
