package com.bitfire.uracer.game.tween;

import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.TweenManager;

import com.bitfire.uracer.URacer;

/** This tweener is a wall-clocked tweener, thus it will NOT take the timeMultiplier modulation into account */
public final class WcTweener {
	private static final TweenManager manager = new TweenManager();

	private WcTweener() {
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
		manager.update( URacer.getLastDeltaMs() );
	}
}
