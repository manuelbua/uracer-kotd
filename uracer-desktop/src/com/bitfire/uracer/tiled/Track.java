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
			Vector2 tmp1 = new Vector2();
			Vector2 tmp2 = new Vector2();
			Vector2 from = new Vector2();
			Vector2 to = new Vector2();
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
						from.x = coords.x;
						to.x = coords.x + tileSize;

						// top
						from.y = to.y = flipY - (coords.y + halfTileSize - halfTrackSize - halfWallSize - wallDistance);
						Box2DFactory.createWall( from, to, wallSize, 0 );

						// bottom
						from.y = to.y = flipY - (coords.y + tileSize - (halfTileSize - halfTrackSize - halfWallSize - wallDistance));
						Box2DFactory.createWall( from, to, wallSize, 0 );
					}
					else
					if( orient.equals( "v" ) )
					{
						from.y = flipY - coords.y;
						to.y = flipY - (coords.y + tileSize);

						// left
						from.x = to.x = coords.x + halfTileSize - halfTrackSize - halfWallSize - wallDistance;
						Box2DFactory.createWall( from, to, wallSize, 0 );

						// right
						from.x = to.x = coords.x + tileSize - (halfTileSize - halfTrackSize - halfWallSize - wallDistance);
						Box2DFactory.createWall( from, to, wallSize, 0 );
					}
					else
					if( orient.equals( "tl" ))
					{
						int steps;
						float angleStep, radStep, cosStep, sinStep;
						float tileAngle;
						float halfWallLength, wallLength;
						float rotX, rotY;

						// build external arc
						steps = 15;
						tileAngle = 90f;
						wallLength = 1f;
						halfWallLength = wallLength / 2f;
						angleStep = tileAngle / (float)(steps);
						radStep = angleStep * MathUtils.degreesToRadians;
						cosStep = (float)Math.cos(radStep);
						sinStep = (float)Math.sin(radStep);

						tmp1.set(-(halfTileSize + halfTrackSize  /* + wallDistance + halfWallSize*/), 0);

						tmp2.set(tmp1);
						tmp2.y -= wallLength;

						for( int step = 0; step < steps; step++ )
						{
							from.x = coords.x + tileSize + tmp1.x;
							from.y = flipY - (coords.y + tmp1.y + tileSize);
							to.x = coords.x + tileSize + tmp2.x;
							to.y = flipY-(coords.y + tmp2.y + tileSize);

							Box2DFactory.createWall(from ,to, wallSize, 0);

							// rotate
							rotX = tmp1.x * cosStep - tmp1.y * sinStep;
							rotY = tmp1.x * sinStep + tmp1.y * cosStep;
							tmp1.set(rotX, rotY);

							rotX = tmp2.x * cosStep - tmp2.y * sinStep;
							rotY = tmp2.x * sinStep + tmp2.y * cosStep;
							tmp2.set(rotX, rotY);
						}

/*
						// build external arc
						steps = 5;
						tileAngle = 90f;
						wallLength = 0.4f;
						angleStep = tileAngle / (float)(steps-1);
						radStep = angleStep * MathUtils.degreesToRadians;
						cosStep = (float)Math.cos(radStep);
						sinStep = (float)Math.sin(radStep);
						angle = 0;

						rInt.set(-(halfTileSize - halfTrackSize - wallDistance - halfWallSize), 0);
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
*/
					}

//					 System.out.format( "%3s ", orient );
				}

//				System.out.println();
			}
		}
	}


}
