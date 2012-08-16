
package com.bitfire.uracer.utils;

import com.badlogic.gdx.utils.TimeUtils;
import com.bitfire.utils.Hash;

public final class UUid {
	public static long incr = 0;

	public static long get () {
		incr++;
		Long time = TimeUtils.nanoTime() + incr;
		return Hash.RSHash(time.toString());
	}

	private UUid () {
	}
}
