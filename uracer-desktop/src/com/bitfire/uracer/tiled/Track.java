package com.bitfire.uracer.tiled;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.tiled.TiledLayer;
import com.badlogic.gdx.graphics.g2d.tiled.TiledMap;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.factories.Box2DFactory;
import com.bitfire.uracer.factories.ModelFactory;
import com.bitfire.uracer.factories.ModelFactory.ModelMesh;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.MapUtils;

/**
 * Creates a perimeter along a given TiledMap (uRacer specific)
 * TODO splice it up, Track and TrackRenderer should be two separate entities
 *
 * @author manuel
 *
 */
public class Track
{
	private TiledMap map;
	private ArrayList<OrthographicAlignedStillModel> meshes = new ArrayList<OrthographicAlignedStillModel>();

//	private ShaderProgram program;

	public Track( TiledMap map )
	{
		this.map = map;

//		String vertexShader =
//				"uniform mat4 u_mvpMatrix;					\n" +
//				"attribute vec4 a_position;					\n" +
//				"attribute vec2 a_texCoord0;				\n" +
//				"varying vec2 v_TexCoord;					\n" +
//				"void main()								\n" +
//				"{											\n" +
//				"	gl_Position = u_mvpMatrix * a_position;	\n" +
//				"	v_TexCoord = a_texCoord0;				\n" +
//				"}											\n";
//
//		String fragmentShader =
//				"#ifdef GL_ES											\n" +
//				"precision mediump float;								\n" +
//				"#endif													\n" +
//				"uniform sampler2D u_texture;							\n" +
//				"varying vec2 v_TexCoord;								\n" +
//				"void main()											\n" +
//				"{														\n" +
//				"	vec4 back = vec4(gl_FragColor.rgb,0);	\n" +
//				"	vec4 track = texture2D( u_texture, v_TexCoord );	\n" +
//				"	gl_FragColor = track;	\n" +
//				"}														\n";
//
//		program = new ShaderProgram( vertexShader, fragmentShader );
//
//		if( program.isCompiled() == false )
//			throw new IllegalStateException( program.getLog() );

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
			float wallSizePx = Convert.mt2px( wallSizeMt );
			float wallDistancePx = Convert.mt2px( wallDistanceMt );

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

						float adj = 2f * Director.scalingStrategy.tileMapZoomFactor;

						// mesh top
						wallMesh = ModelFactory.create( ModelMesh.WallHorizontal,
							meshCoords.x,
							meshCoords.y + (halfTileSizePx - halfTrackSizePx - wallSizePx - wallDistancePx) - adj, 1f );
						meshes.add( wallMesh );

						// mesh bottom
						wallMesh = ModelFactory.create( ModelMesh.WallHorizontal,
							meshCoords.x,
							meshCoords.y + (halfTileSizePx + halfTrackSizePx + wallDistancePx) - adj, 1f);
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

						float adj = 1f * Director.scalingStrategy.tileMapZoomFactor;

						// mesh left
						wallMesh = ModelFactory.create( ModelMesh.WallHorizontal,
								meshCoords.x + (halfTileSizePx - halfTrackSizePx - wallSizePx - wallDistancePx) - adj,
								meshCoords.y + tileSizePx - adj, 1f );
						wallMesh.setRotation( 90, 0, 0, 1 );
						meshes.add( wallMesh );

						// mesh right
						wallMesh = ModelFactory.create( ModelMesh.WallHorizontal,
							meshCoords.x + (halfTileSizePx + halfTrackSizePx + wallDistancePx) - 2 * adj,
							meshCoords.y + tileSizePx - adj, 1f );
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

						float adj = 1f * Director.scalingStrategy.tileMapZoomFactor;

						// external mesh
						wallMesh = ModelFactory.create( ModelMesh.WallTopRightOuter,
								meshCoords.x + (halfTileSizePx - halfTrackSizePx - wallSizePx - wallDistancePx) - adj,
								meshCoords.y + tileSizePx + adj, 1f );
						wallMesh.setRotation( 90, 0, 0, 1 );
						meshes.add( wallMesh );

						// internal mesh
						wallMesh = ModelFactory.create( ModelMesh.WallTopRightInner,
								meshCoords.x + (halfTileSizePx + halfTrackSizePx + wallDistancePx) - 2*adj,
								meshCoords.y + tileSizePx - 2*adj, 1f );
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

						float adj = 1f * Director.scalingStrategy.tileMapZoomFactor;

						// external mesh
						wallMesh = ModelFactory.create( ModelMesh.WallTopRightOuter,
								meshCoords.x - adj,
								meshCoords.y + (halfTileSizePx - halfTrackSizePx - wallSizePx - wallDistancePx) - 2*adj, 1f );
						meshes.add( wallMesh );

						// internal mesh
						wallMesh = ModelFactory.create( ModelMesh.WallTopRightInner,
								meshCoords.x,
								meshCoords.y + (halfTileSizePx + halfTrackSizePx + wallDistancePx) - adj, 1f );
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

						float adj = 1f * Director.scalingStrategy.tileMapZoomFactor;

						// external mesh
						wallMesh = ModelFactory.create( ModelMesh.WallTopRightOuter,
								meshCoords.x + tileSizePx + adj,
								meshCoords.y + tileSizePx - (halfTileSizePx - halfTrackSizePx - wallSizePx - wallDistancePx), 1f );
						wallMesh.setRotation( 180, 0, 0, 1 );
						meshes.add( wallMesh );

						// internal mesh
						wallMesh = ModelFactory.create( ModelMesh.WallTopRightInner,
								meshCoords.x + tileSizePx - adj,
								meshCoords.y + tileSizePx - (halfTileSizePx + halfTrackSizePx + wallDistancePx), 1f );
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

						float adj = 1f * Director.scalingStrategy.tileMapZoomFactor;

						// external mesh
						wallMesh = ModelFactory.create( ModelMesh.WallTopRightOuter,
								meshCoords.x + tileSizePx - (halfTileSizePx - halfTrackSizePx - wallSizePx - wallDistancePx),
								meshCoords.y - 2f * adj, 1f );
						wallMesh.setRotation( -90, 0, 0, 1 );
						meshes.add( wallMesh );

						// internal mesh
						wallMesh = ModelFactory.create( ModelMesh.WallTopRightInner,
								meshCoords.x + tileSizePx - (halfTileSizePx + halfTrackSizePx + wallDistancePx) + adj,
								meshCoords.y - adj, 1f );
						wallMesh.setRotation( -90, 0, 0, 1 );
						meshes.add( wallMesh );
}
				}
			}
		}
	}

	public void render( GL20 gl )
	{
		OrthographicAlignedStillModel m;

		gl.glEnable( GL20.GL_BLEND );
//		gl.glBlendFunc( GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA );

		// Use color modulation. glTexEnvx(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE); Then set alpha component of the
		// color to desired. glColor4f(255, 255, 255, alpha);

		int size = meshes.size();
		for( int i = 0; i < size; i++ )
		{
			m = meshes.get( i );
			m.render( gl );
		}

		gl.glDisable( GL20.GL_BLEND );
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
