package com.bitfire.uracer.tiled;

import com.badlogic.gdx.graphics.g2d.tiled.TiledLayer;
import com.badlogic.gdx.graphics.g2d.tiled.TiledMap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.utils.Box2DFactory;
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
	 * Inspect the TiledMap, create a perimeter out of each tile with Box2D's
	 * static bodys
	 */
	private void createPerimeter()
	{
		if( MapUtils.hasLayer( "track" ) )
		{
			float wallSize = 0.2f;
			float wallDistance = 0.3f;

			float trackSize = Convert.px2mt( 144f );
			float tileSize = Convert.px2mt( map.tileWidth );
			float halfTrackSize = trackSize / 2f;
			float halfTileSize = tileSize / 2f;
			float halfWallSize = wallSize / 2f;
			float flipY = Director.worldSizeScaledMt.y;

			Vector2 coords = new Vector2();
			Vector2 rExt = new Vector2();
			Vector2 rInt = new Vector2();
			TiledLayer layer = MapUtils.getLayer( "track" );

			for( int y = 0; y < map.height; y++ )
			{
				for( int x = 0; x < map.width; x++ )
				{
					int id = layer.tiles[y][x];
					String orient = map.getTileProperty( id, "orient" );
					if( orient == null )
					{
//						 System.out.format( "    " );
						continue;
					}

					// find out world mt coordinates for this tile's top-left
					// corner
					coords.set( x * map.tileWidth, y * map.tileHeight );
					coords = Convert.px2mt( coords );

					if( orient.equals( "h" ) )
					{
						float hY1 = coords.y + halfTileSize - halfTrackSize - halfWallSize - wallDistance;
						float hY2 = coords.y + halfTileSize - halfTrackSize + halfWallSize - wallDistance;

						// top
//						Box2DFactory.createWall( coords.x, flipY-hY1, coords.x + tileSize, flipY-hY2, 0, 0 );

						// bottom
//						Box2DFactory.createWall( coords.x, flipY-(tileSize-hY1), coords.x + tileSize, flipY-(tileSize-hY2), 0, 0 );
					}
					else
					if( orient.equals( "v" ) )
					{
						float hX1 = coords.x + halfTileSize - halfTrackSize - halfWallSize - wallDistance;
						float hX2 = coords.x + halfTileSize - halfTrackSize + halfWallSize - wallDistance;

						// left
//						Box2DFactory.createWall( hX1, flipY-coords.y, hX2, flipY-(coords.y+tileSize), 0, 0 );

						// right
//						Box2DFactory.createWall( tileSize - hX1, flipY-coords.y, tileSize - hX2, flipY-(coords.y+tileSize), 0, 0 );
					}
					else
					if( orient.equals( "tl" ))
					{
						int steps;
						float angleStep, radStep, cosStep, sinStep, angle;
						float tileAngle;
						float wallLength;

						// build external arc
						steps = 15;
						tileAngle = 90f;
						wallLength = 1f;
						angleStep = tileAngle / (float)(steps-1);
						radStep = angleStep * MathUtils.degreesToRadians;
						cosStep = (float)Math.cos(radStep);
						sinStep = (float)Math.sin(radStep);

						angle = 0;//(90f / (float)(steps*wallLength));
						rExt.set(-(halfTileSize + halfTrackSize + wallDistance + wallSize), 0);

						for( int step = 0; step < steps; step++ )
						{
							System.out.println("building for " + angle);
							Box2DFactory.createWall(
									coords.x + tileSize + rExt.x           , flipY-(coords.y + rExt.y              + tileSize),
									coords.x + tileSize + rExt.x + wallSize, flipY-(coords.y + rExt.y - wallLength + tileSize),
									 -angle * MathUtils.degreesToRadians, 0 );

							angle += angleStep;

							// rotate
							float rotX = rExt.x * cosStep - rExt.y * sinStep;
							float rotY = rExt.x * sinStep + rExt.y * cosStep;
							rExt.set(rotX, rotY);
						}

						// build external arc
						steps = 5;
						tileAngle = 90f;
						wallLength = 0.4f;
						angleStep = tileAngle / (float)(steps-1);
						radStep = angleStep * MathUtils.degreesToRadians;
						cosStep = (float)Math.cos(radStep);
						sinStep = (float)Math.sin(radStep);
						angle = 0;

						rInt.set(-(halfTileSize - halfTrackSize - wallDistance - wallSize), 0);
						for( int step = 0; step < steps; step++ )
						{
							Box2DFactory.createWall(
									coords.x + tileSize + rInt.x           , flipY-(coords.y + rInt.y              + tileSize),
									coords.x + tileSize + rInt.x + wallSize, flipY-(coords.y + rInt.y - wallLength + tileSize),
									 -angle * MathUtils.degreesToRadians, 0 );

							angle += angleStep;

							// rotate
							float rotX = rInt.x * cosStep - rInt.y * sinStep;
							float rotY = rInt.x * sinStep + rInt.y * cosStep;
							rInt.set(rotX, rotY);
						}
					}

//					 System.out.format( "%3s ", orient );
				}

				System.out.println();
			}
		}
	}


}
