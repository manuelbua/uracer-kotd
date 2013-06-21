
package com.bitfire.uracer.game;

import com.bitfire.uracer.utils.AMath;

public final class GameplaySettings {
	public enum TimeDilateInputMode {
		// @off
		Toggle,				// (spacebar to act on desktop, touch to enable + touch to disable acting on device
		TouchAndRelease,	// touch to act, release to stop acting
		// @on
	}

	public static final float DampingFriction = AMath.damping(0.975f);
	public static final float DampingKeyboardKeys = AMath.damping(0.9f);

	// maximum amount of seconds for the wrong way detector before the lap is completely invalidated
	public static final float MaxSecondsWrongWayDetector = 1.0f;

	// a replay is discarded if its length is less than the specified seconds
	public static final float ReplayMinDurationSecs = 4f;

	private GameplaySettings () {
	}
}
