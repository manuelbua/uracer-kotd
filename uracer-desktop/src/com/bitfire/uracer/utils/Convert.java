package com.bitfire.uracer.utils;

import com.badlogic.gdx.graphics.g2d.tiled.TiledMap;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.tiled.ScalingStrategy;

public class Convert {
	private static TiledMap tileMap;

	public static float scaledTilesize, invScaledTilesize;
	private static float invZoomFactor;

	private static Vector2 ret = new Vector2();
	private static Vector2 retTile = new Vector2();
	private static Vector2 retPx = new Vector2();

	public static void init( ScalingStrategy strategy, TiledMap map ) {
		tileMap = map;
		invZoomFactor = strategy.invTileMapZoomFactor;
		scaledTilesize = tileMap.tileWidth * invZoomFactor;
		invScaledTilesize = 1f / scaledTilesize;
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

	public static Vector2 tileToMt( int tilex, int tiley ) {
		return px2mt( tileToPx( tilex, tiley ) );
	}

	public static Vector2 tileToPx( int tilex, int tiley ) {
		retTile.set( tilex * scaledTilesize, (tileMap.height - tiley) * scaledTilesize );
		return retTile;
	}

	public static Vector2 pxToTile( float x, float y ) {
		retTile.set( x, y );
		retTile.mul( invScaledTilesize );
		retTile.y = tileMap.height - retTile.y;
		VMath.truncateToInt( retTile );
		return retTile;
	}

	public static Vector2 mtToTile( float x, float y ) {
		retPx.set( mt2px( x ), mt2px( y ) );
		retPx = pxToTile( retPx.x, retPx.y );
		return retPx;
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
