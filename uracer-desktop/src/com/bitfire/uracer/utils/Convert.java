package com.bitfire.uracer.utils;

import com.badlogic.gdx.graphics.g2d.tiled.TiledMap;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.tiled.ScalingStrategy;

public class Convert
{
	// for return values
	private static TiledMap tileMap;

	private static float scaled_tilesize;
	private static float invZoomFactor;

	public static void init(ScalingStrategy strategy, TiledMap map)
	{
		tileMap = map;
		invZoomFactor = strategy.invTileMapZoomFactor;
		scaled_tilesize = tileMap.tileWidth * invZoomFactor;

		ret = new Vector2();
		retTile = new Vector2();
		retPx = new Vector2();
	}

	//
	// meters <-> pixels
	// (pixels domain is scaled)
	//

	private static Vector2 ret;
	public static float mt2px( float v )		{ return v * Config.PixelsPerMeter; }
	public static float px2mt( float v )		{ return v / Config.PixelsPerMeter; }
	public static Vector2 mt2px( Vector2 v )	{ ret.set(v.x * Config.PixelsPerMeter, v.y * Config.PixelsPerMeter); return ret; }
	public static Vector2 px2mt( Vector2 v )	{ ret.set(v.x / Config.PixelsPerMeter, v.y / Config.PixelsPerMeter); return ret; }

	private static Vector2 retTile;
	public static Vector2 tileToMt( int tilex, int tiley )
	{
		retTile.set( tilex * scaled_tilesize, (tileMap.height - tiley) * scaled_tilesize );
		return px2mt(retTile);
	}

	public static Vector2 tileToPx( int tilex, int tiley )
	{
		retTile.set( tilex * scaled_tilesize, (tileMap.height - tiley) * scaled_tilesize );
		return retTile;
	}

	public static Vector2 pxToTile( float x, float y )
	{
		retTile.set(x, y);
		retTile.mul( 1f / scaled_tilesize );
		retTile.y = tileMap.height - retTile.y;
		VMath.truncateToInt( retTile );
		return retTile;
	}

	public static Vector2 mtToTile( float x, float y )
	{
		retPx.set( mt2px(x), mt2px(y) );
		retPx = pxToTile(retPx.x, retPx.y);
		return retPx;
	}

	public static float scaledPixels( float pixels )
	{
		return pixels * invZoomFactor;
	}

	private static Vector2 retPx;
	public static Vector2 scaledPixels(Vector2 pixels)
	{
		retPx.set(pixels);
		retPx.mul( invZoomFactor );
		return retPx;
	}

	public static Vector2 scaledPixels(float a, float b)
	{
		retPx.set( a, b );
		return scaledPixels(retPx);
	}
}
