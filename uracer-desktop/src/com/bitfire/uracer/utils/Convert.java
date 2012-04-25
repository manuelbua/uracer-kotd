package com.bitfire.uracer.utils;

import com.badlogic.gdx.math.Vector2;

public final class Convert {
	private static float invZoomFactor;
	private static float pixelsPerMeter;

	private static Vector2 ret = new Vector2();
	private static Vector2 retPx = new Vector2();

	private Convert() {
	}

	public static void init( float invZoomFactor, float pixelsPerMeter ) {
		Convert.invZoomFactor = invZoomFactor;
		Convert.pixelsPerMeter = pixelsPerMeter;
	}

	public static float mt2px( float v ) {
		return v * Convert.pixelsPerMeter;
	}

	public static float px2mt( float v ) {
		return v / Convert.pixelsPerMeter;
	}

	public static Vector2 mt2px( final Vector2 v ) {
		ret.set( v.x * Convert.pixelsPerMeter, v.y * Convert.pixelsPerMeter );
		return ret;
	}

	public static Vector2 px2mt( final Vector2 v ) {
		ret.set( v.x / Convert.pixelsPerMeter, v.y / Convert.pixelsPerMeter );
		return ret;
	}

	public static float scaledPixels( float pixels ) {
		return pixels * invZoomFactor;
	}

	public static Vector2 scaledPixels( final Vector2 pixels ) {
		retPx.set( pixels );
		retPx.mul( invZoomFactor );
		return retPx;
	}

	public static Vector2 scaledPixels( float a, float b ) {
		retPx.set( a, b );
		return scaledPixels( retPx );
	}
}
