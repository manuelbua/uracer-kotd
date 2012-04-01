package com.bitfire.uracer.utils;

import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.game.GameData;

public class Convert {
	private static float invZoomFactor;

	private static Vector2 ret = new Vector2();
	private static Vector2 retPx = new Vector2();

	public static void init() {
		invZoomFactor = GameData.scalingStrategy.invTileMapZoomFactor;
	}

	//
	// meters <-> pixels
	// (pixels domain is scaled)
	//

	public static float mt2px( float v ) {
		return v * Config.Physics.PixelsPerMeter;
	}

	public static float px2mt( float v ) {
		return v / Config.Physics.PixelsPerMeter;
	}

	public static Vector2 mt2px( final Vector2 v ) {
		ret.set( v.x * Config.Physics.PixelsPerMeter, v.y * Config.Physics.PixelsPerMeter );
		return ret;
	}

	public static Vector2 px2mt( final Vector2 v ) {
		ret.set( v.x / Config.Physics.PixelsPerMeter, v.y / Config.Physics.PixelsPerMeter );
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
