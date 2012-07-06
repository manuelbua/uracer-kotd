package com.bitfire.uracer.utils;

import com.badlogic.gdx.math.Vector2;

public final class Convert {
	private static float invZoomFactor;
	private static float invPixelsPerMeter;

	private static float ppmMulInvZoomFactor;
	private static float invPpmMulZoomFactor;

	private static Vector2 ret = new Vector2();
	private static Vector2 retPx = new Vector2();

	private Convert() {
	}

	public static void init( float zoomFactor, float pixelsPerMeter ) {
		Convert.invZoomFactor = 1f / zoomFactor;
		Convert.invPixelsPerMeter = 1f / pixelsPerMeter;

		Convert.ppmMulInvZoomFactor = pixelsPerMeter * invZoomFactor;
		Convert.invPpmMulZoomFactor = invPixelsPerMeter * zoomFactor;
	}

	public static float mt2px( float v ) {
		return v * ppmMulInvZoomFactor;
	}

	public static Vector2 mt2px( final Vector2 v ) {
		ret.set( v.x * ppmMulInvZoomFactor, v.y * ppmMulInvZoomFactor);
		return ret;
	}

	public static float px2mt( float v ) {
		return v * invPpmMulZoomFactor;
	}

	public static Vector2 px2mt( final Vector2 v ) {
		ret.set( v.x * invPixelsPerMeter, v.y * invPixelsPerMeter );
		return ret;
	}

	/* convert pixels to meters without scaling the specified pixels */
	public static float upx2mt( float v ) {
		return v * invPixelsPerMeter;
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
