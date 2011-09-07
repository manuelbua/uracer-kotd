package com.bitfire.uracer.tiled;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.tiled.TiledLayer;
import com.badlogic.gdx.graphics.g2d.tiled.TiledMap;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.factories.Box2DFactory;
import com.bitfire.uracer.factories.ModelFactory;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.MapUtils;

public class Track
{
	private TiledMap map;
	private ArrayList<OrthographicAlignedStillModel> meshes = new ArrayList<OrthographicAlignedStillModel>();

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
		meshes.clear();

		if( MapUtils.hasLayer( "track" ) )
		{
			// physics
			float restitution = 0.15f;

			// common
			float wallSizeMt = 0.3f;
			float wallDistanceMt = 0.3f;

			float wallSizePx = Convert.mt2px( 0.3f );
			float wallDistancePx = Convert.mt2px( 0.3f );

			wallSizeMt *= Director.scalingStrategy.invTileMapZoomFactor;
			wallDistanceMt *= Director.scalingStrategy.invTileMapZoomFactor;

			// 224px tileset (mt)
			float trackSizeMt = Convert.px2mt( 144f ) * Director.scalingStrategy.invTileMapZoomFactor;
			float tileSizeMt = Convert.px2mt( map.tileWidth ) * Director.scalingStrategy.invTileMapZoomFactor;
			float halfTrackSizeMt = trackSizeMt / 2f;
			float halfTileSizeMt = tileSizeMt / 2f;
			float halfWallSizeMt = wallSizeMt / 2f;
			float flipYMt = Director.worldSizeScaledMt.y;

			// 224px tileset (px, unscaled since OrthoMesh already scale on its own)
			float trackSizePx = 144f;
			float tileSizePx = map.tileWidth;
			float halfTrackSizePx = trackSizePx / 2f;
			float halfTileSizePx = tileSizePx / 2f;
//			float halfWallSizePx = wallSizePx / 2f;
//			float flipYPx = Director.worldSizeScaledPx.y;

			Vector2 coords = new Vector2();
			Vector2 meshCoords = new Vector2();

			// angular wall
			Vector2 tmp1 = new Vector2();
			Vector2 tmp2 = new Vector2();
			Vector2 from = new Vector2();
			Vector2 to = new Vector2();
			Vector2 rotOffset = new Vector2();
			float outerLumpLen = 1.9f;
			float innerLumpLen = 0.8f;

			OrthographicAlignedStillModel wallMesh = null;
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
					meshCoords.set(coords);

					coords = Convert.px2mt( coords );
					coords.mul( Director.scalingStrategy.invTileMapZoomFactor );

					if( orient.equals( "h" ) )
					{
						from.x = coords.x;
						to.x = coords.x + tileSizeMt;

						// shape top
						from.y = to.y = flipYMt - (coords.y + halfTileSizeMt - halfTrackSizeMt - halfWallSizeMt - wallDistanceMt);
						Box2DFactory.createWall( from, to, wallSizeMt, restitution );

						// shape bottom
						from.y = to.y = flipYMt - (coords.y + tileSizeMt - (halfTileSizeMt - halfTrackSizeMt - halfWallSizeMt - wallDistanceMt));
						Box2DFactory.createWall( from, to, wallSizeMt, restitution );

						// mesh top
						wallMesh = ModelFactory.create( ModelFactory.WallHorizontal,
							meshCoords.x,
							meshCoords.y + (halfTileSizePx - halfTrackSizePx - wallSizePx - wallDistancePx - 2), 1f );
						meshes.add( wallMesh );

						// mesh bottom
						wallMesh = ModelFactory.create( ModelFactory.WallHorizontal,
							meshCoords.x,
							meshCoords.y + (halfTileSizePx + halfTrackSizePx + wallDistancePx - 1), 1f);
						meshes.add( wallMesh );
					}
					else
					if( orient.equals( "v" ) )
					{
						from.y = flipYMt - coords.y;
						to.y = flipYMt - (coords.y + tileSizeMt);

						// shape left
						from.x = to.x = coords.x + halfTileSizeMt - halfTrackSizeMt - halfWallSizeMt - wallDistanceMt;
						Box2DFactory.createWall( from, to, wallSizeMt, restitution );

						// shape right
						from.x = to.x = coords.x + tileSizeMt - (halfTileSizeMt - halfTrackSizeMt - halfWallSizeMt - wallDistanceMt);
						Box2DFactory.createWall( from, to, wallSizeMt, restitution );

						// mesh left
						wallMesh = ModelFactory.create( ModelFactory.WallHorizontal,
								meshCoords.x + (halfTileSizePx - halfTrackSizePx - wallSizePx - wallDistancePx - 1),
								meshCoords.y + tileSizePx - 2, 1f );
						wallMesh.setRotation( 90, 0, 0, 1 );
						meshes.add( wallMesh );

						// mesh right
						wallMesh = ModelFactory.create( ModelFactory.WallHorizontal,
							meshCoords.x + (halfTileSizePx + halfTrackSizePx + wallDistancePx),
							meshCoords.y + tileSizePx - 2, 1f );
						wallMesh.setRotation( 90, 0, 0, 1 );
						meshes.add( wallMesh );
					}
					else
					if( orient.equals( "tl" ))
					{
						tmp1.set( -(halfTileSizeMt + halfTrackSizeMt + wallDistanceMt + halfWallSizeMt), 0 );	// unit circle radius
						tmp2.set( coords.x + tileSizeMt, flipYMt - coords.y - tileSizeMt );	// offset
						rotOffset.set(0f, 1f).mul(Director.scalingStrategy.tileMapZoomFactor);	// rotational offset

						// external arc
						Box2DFactory.createAngularWall( tmp1, tmp2, wallSizeMt, outerLumpLen, 90f, 10, rotOffset, restitution, false );

						// internal arc
						tmp1.set( -(halfTileSizeMt - halfTrackSizeMt - wallDistanceMt - halfWallSizeMt), 0 );	// unit circle radius
						Box2DFactory.createAngularWall( tmp1, tmp2, wallSizeMt, innerLumpLen, 90f, 4, rotOffset, restitution, false );

						// external mesh
						wallMesh = ModelFactory.create( ModelFactory.WallTopRightOuter,
								meshCoords.x,
								meshCoords.y + tileSizePx - 2, 1f );
						wallMesh.setRotation( 90, 0, 0, 1 );
						meshes.add( wallMesh );
					}
					else
					if( orient.equals( "tr" ))
					{
						tmp1.set( (halfTileSizeMt + halfTrackSizeMt + wallDistanceMt + halfWallSizeMt), 0 );	// unit circle radius
						tmp2.set( coords.x, flipYMt - coords.y - tileSizeMt );	// offset
						rotOffset.set(0f, -1f).mul(Director.scalingStrategy.tileMapZoomFactor);	// rotational offset

						// external arc
						Box2DFactory.createAngularWall( tmp1, tmp2, wallSizeMt, outerLumpLen, -90f, 10, rotOffset, restitution, false );

						// internal arc
						tmp1.set( (halfTileSizeMt - halfTrackSizeMt - wallDistanceMt - halfWallSizeMt), 0 );	// unit circle radius
						Box2DFactory.createAngularWall( tmp1, tmp2, wallSizeMt, innerLumpLen, -90f, 4, rotOffset, restitution, false );

						// external mesh
						wallMesh = ModelFactory.create( ModelFactory.WallTopRightOuter,
								meshCoords.x,
								meshCoords.y - 2, 1f );
						meshes.add( wallMesh );
					}
					else
					if( orient.equals( "bl" ))
					{
						tmp1.set( -(halfTileSizeMt + halfTrackSizeMt + wallDistanceMt + halfWallSizeMt), 0 );	// unit circle radius
						tmp2.set( coords.x + tileSizeMt, flipYMt - coords.y );	// offset
						rotOffset.set(0f, 1f).mul(Director.scalingStrategy.tileMapZoomFactor);	// rotational offset

						// external arc
						Box2DFactory.createAngularWall( tmp1, tmp2, wallSizeMt, -outerLumpLen, -90f, 10, rotOffset, restitution, false );

						// internal arc
						tmp1.set( -(halfTileSizeMt - halfTrackSizeMt - wallDistanceMt - halfWallSizeMt), 0 );	// unit circle radius
						Box2DFactory.createAngularWall( tmp1, tmp2, wallSizeMt, -innerLumpLen, -90f, 4, rotOffset, restitution, false );

						// external mesh
						wallMesh = ModelFactory.create( ModelFactory.WallTopRightOuter,
								meshCoords.x + tileSizePx,
								meshCoords.y + tileSizePx - 2, 1f );
						wallMesh.setRotation( 180, 0, 0, 1 );
						meshes.add( wallMesh );
					}
					else
					if( orient.equals( "br" ))
					{
						tmp1.set( (halfTileSizeMt + halfTrackSizeMt + wallDistanceMt + halfWallSizeMt), 0 );	// unit circle radius
						tmp2.set( coords.x, flipYMt - coords.y );	// offset
						rotOffset.set(0f, -1f).mul(Director.scalingStrategy.tileMapZoomFactor);	// rotational offset

						// external arc
						Box2DFactory.createAngularWall( tmp1, tmp2, wallSizeMt, -outerLumpLen, 90f, 10, rotOffset, restitution, false );

						// internal arc
						tmp1.set( (halfTileSizeMt - halfTrackSizeMt - wallDistanceMt - halfWallSizeMt), 0 );	// unit circle radius
						Box2DFactory.createAngularWall( tmp1, tmp2, wallSizeMt, -innerLumpLen, 90f, 4, rotOffset, restitution, false );

						// external mesh
						wallMesh = ModelFactory.create( ModelFactory.WallTopRightOuter,
								meshCoords.x + tileSizePx + 2,
								meshCoords.y - 2, 1f );
						wallMesh.setRotation( -90, 0, 0, 1 );
						meshes.add( wallMesh );
					}
				}
			}
		}
	}

	public boolean hasMeshes()
	{
		return meshes.size() > 0;
	}

	public ArrayList<OrthographicAlignedStillModel> getMeshes()
	{
		return meshes;
	}
}
