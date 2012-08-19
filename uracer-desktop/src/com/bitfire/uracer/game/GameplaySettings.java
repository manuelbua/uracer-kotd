
package com.bitfire.uracer.game;

import com.bitfire.uracer.utils.AMath;

public final class GameplaySettings {
	// settings
	public static final float DampingFriction = AMath.damping(0.975f);

	// a replay is discarded if its length is less than the specified seconds
	public static final float ReplayMinDurationSecs = 2f;

	private GameplaySettings () {
	}
}
