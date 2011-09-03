package com.bitfire.uracer.tiled;

import com.badlogic.gdx.graphics.g2d.tiled.TiledLayer;
import com.badlogic.gdx.graphics.g2d.tiled.TiledMap;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.factories.Box2DFactory;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.MapUtils;

public class Track
{
	private TiledMap map;

	public Track( TiledMap map )
	{
		this.map = map;

		createPerimeter();
	}

	/**
	 * Traverse the map and create a Box2D's bodys manifold perimeter out of each tile.
	 */
	private void createPerimeter()
	{
		if( MapUtils.hasLayer( "track" ) )
		{
			// common
			float wallSize = 0.3f;
			float wallDistance = 0.3f;
			float restitution = 0.15f;

			wallSize *= Director.scalingStrategy.invTileMapZoomFactor;
			wallDistance *= Director.scalingStrategy.invTileMapZoomFactor;

			// 224px tileset
			float trackSize = Convert.px2mt( 144f ) * Director.scalingStrategy.invTileMapZoomFactor;
			float tileSize = Convert.px2mt( map.tileWidth ) * Director.scalingStrategy.invTileMapZoomFactor;
			float halfTrackSize = trackSize / 2f;
			float halfTileSize = tileSize / 2f;
			float halfWallSize = wallSize / 2f;
			float flipY = Director.worldSizeScaledMt.y;

			Vector2 coords = new Vector2();

			// angular wall
			Vector2 tmp1 = new Vector2();
			Vector2 tmp2 = new Vector2();
			Vector2 from = new Vector2();
			Vector2 to = new Vector2();
			Vector2 rotOffset = new Vector2();
			float outerLumpLen = 1.9f;
			float innerLumpLen = 0.8f;

			TiledLayer layer = MapUtils.getLayer( "track" );

			for( int y = 0; y < map.height; y++ )
			{
				for( int x = 0; x < map.width; x++ )
				{
					int id = layer.tiles[y][x];
					String orient = map.getTileProperty( id, "orient" );
					if( orient == null )
					{
						continue;
					}

					// find out world mt coordinates for this tile's top-left
					// corner
					coords.set( x * map.tileWidth, y * map.tileHeight );
					coords = Convert.px2mt( coords );
					coords.mul( Director.scalingStrategy.invTileMapZoomFactor );

					if( orient.equals( "h" ) )
					{
						from.x = coords.x;
						to.x = coords.x + tileSize;

						// top
						from.y = to.y = flipY - (coords.y + halfTileSize - halfTrackSize - halfWallSize - wallDistance);
						Box2DFactory.createWall( from, to, wallSize, restitution );

						// bottom
						from.y = to.y = flipY - (coords.y + tileSize - (halfTileSize - halfTrackSize - halfWallSize - wallDistance));
						Box2DFactory.createWall( from, to, wallSize, restitution );
					}
					else
					if( orient.equals( "v" ) )
					{
						from.y = flipY - coords.y;
						to.y = flipY - (coords.y + tileSize);

						// left
						from.x = to.x = coords.x + halfTileSize - halfTrackSize - halfWallSize - wallDistance;
						Box2DFactory.createWall( from, to, wallSize, restitution );

						// right
						from.x = to.x = coords.x + tileSize - (halfTileSize - halfTrackSize - halfWallSize - wallDistance);
						Box2DFactory.createWall( from, to, wallSize, restitution );
					}
					else
					if( orient.equals( "tl" ))
					{
						// external arc
						tmp1.set( -(halfTileSize + halfTrackSize + wallDistance + halfWallSize), 0 );	// unit circle radius
						tmp2.set( coords.x + tileSize, flipY - coords.y - tileSize );	// offset
						Box2DFactory.createAngularWall( tmp1, tmp2, wallSize, outerLumpLen, 90f, 10, rotOffset.set(0f, 1f), restitution, false );

						// internal arc
						tmp1.set( -(halfTileSize - halfTrackSize - wallDistance - halfWallSize), 0 );	// unit circle radius
						Box2DFactory.createAngularWall( tmp1, tmp2, wallSize, innerLumpLen, 90f, 4, rotOffset.set(0f, 1f), restitution, false );
					}
					else
					if( orient.equals( "tr" ))
					{
						// external arc
						tmp1.set( (halfTileSize + halfTrackSize + wallDistance + halfWallSize), 0 );	// unit circle radius
						tmp2.set( coords.x, flipY - coords.y - tileSize );	// offset
						Box2DFactory.createAngularWall( tmp1, tmp2, wallSize, outerLumpLen, -90f, 10, rotOffset.set(0f, -1f), restitution, false );

						// internal arc
						tmp1.set( (halfTileSize - halfTrackSize - wallDistance - halfWallSize), 0 );	// unit circle radius
						Box2DFactory.createAngularWall( tmp1, tmp2, wallSize, innerLumpLen, -90f, 4, rotOffset.set(0f, -1f), restitution, false );
					}
					else
					if( orient.equals( "bl" ))
					{
						// external arc
						tmp1.set( -(halfTileSize + halfTrackSize + wallDistance + halfWallSize), 0 );	// unit circle radius
						tmp2.set( coords.x + tileSize, flipY - coords.y );	// offset
						Box2DFactory.createAngularWall( tmp1, tmp2, wallSize, -outerLumpLen, -90f, 10, rotOffset.set(0f, 1f), restitution, false );

						// internal arc
						tmp1.set( -(halfTileSize - halfTrackSize - wallDistance - halfWallSize), 0 );	// unit circle radius
						Box2DFactory.createAngularWall( tmp1, tmp2, wallSize, -innerLumpLen, -90f, 4, rotOffset.set(0f, 1f), restitution, false );
					}
					else
					if( orient.equals( "br" ))
					{
						// external arc
						tmp1.set( (halfTileSize + halfTrackSize + wallDistance + halfWallSize), 0 );	// unit circle radius
						tmp2.set( coords.x, flipY - coords.y );	// offset
						Box2DFactory.createAngularWall( tmp1, tmp2, wallSize, -outerLumpLen, 90f, 10, rotOffset.set(0f, -1f), restitution, false );

						// internal arc
						tmp1.set( (halfTileSize - halfTrackSize - wallDistance - halfWallSize), 0 );	// unit circle radius
						Box2DFactory.createAngularWall( tmp1, tmp2, wallSize, -innerLumpLen, 90f, 4, rotOffset.set(0f, -1f), restitution, false );
					}
				}
			}
		}
	}

}
