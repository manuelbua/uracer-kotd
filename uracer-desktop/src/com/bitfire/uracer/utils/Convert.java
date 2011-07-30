package com.bitfire.uracer.utils;

import com.badlogic.gdx.graphics.g2d.tiled.TiledMap;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.testtilemap.ScalingStrategy;
import com.bitfire.uracer.Config;

public class Convert
{
	// for return values
	private static ScalingStrategy strategy;
	private static TiledMap tileMap;

	private static float scaled_tilesize;

	public static void init(ScalingStrategy s, TiledMap map)
	{
		strategy = s;
		tileMap = map;
		scaled_tilesize = tileMap.tileWidth / strategy.tileMapZoomFactor;

		ret = new Vector2();
		retTile = new Vector2();
	}

	//
	// meters <-> pixels
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

}
