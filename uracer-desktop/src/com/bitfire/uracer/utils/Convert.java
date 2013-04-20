
package com.bitfire.uracer.utils;

import com.badlogic.gdx.math.Vector2;

public final class Convert {
	private static float invPixelsPerMeter;
	private static float pixelsPerMeter;

	private static Vector2 retMt = new Vector2();
	private static Vector2 retPx = new Vector2();

	private Convert () {
	}

	public static void init (float pixelsPerMeter) {
		Convert.pixelsPerMeter = pixelsPerMeter;
		Convert.invPixelsPerMeter = 1f / pixelsPerMeter;
	}

	public static float mt2px (float v) {
		return v * pixelsPerMeter;
	}

	public static Vector2 mt2px (final Vector2 v) {
		retPx.set(v.x * pixelsPerMeter, v.y * pixelsPerMeter);
		return retPx;
	}

	public static float px2mt (float v) {
		return v * invPixelsPerMeter;
	}

	public static Vector2 px2mt (final Vector2 v) {
		retMt.set(v.x * invPixelsPerMeter, v.y * invPixelsPerMeter);
		return retMt;
	}
}
