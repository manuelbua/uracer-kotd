package com.bitfire.uracer.game;

import java.util.ArrayList;
import java.util.List;

import box2dLight.ConeLight;
import box2dLight.PointLight;
import box2dLight.RayHandler;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.tiled.TiledLayer;
import com.badlogic.gdx.graphics.g2d.tiled.TiledLoader;
import com.badlogic.gdx.graphics.g2d.tiled.TiledMap;
import com.badlogic.gdx.graphics.g2d.tiled.TiledObject;
import com.badlogic.gdx.graphics.g2d.tiled.TiledObjectGroup;
import com.badlogic.gdx.graphics.g3d.materials.Material;
import com.badlogic.gdx.graphics.g3d.materials.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.still.StillModel;
import com.badlogic.gdx.graphics.g3d.model.still.StillSubMesh;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.game.collisions.CollisionFilters;
import com.bitfire.uracer.game.data.GameData;
import com.bitfire.uracer.game.models.ModelFactory;
import com.bitfire.uracer.game.models.OrthographicAlignedStillModel;
import com.bitfire.uracer.game.models.TrackTrees;
import com.bitfire.uracer.game.models.TrackWalls;
import com.bitfire.uracer.game.models.TreeStillModel;
import com.bitfire.uracer.game.physics.Box2DFactory;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.Convert;

public final class GameWorld {
	// statistics
	public static int TotalMeshes = 0;

	// level data
	public String name = "no-level-loaded";
	public TiledMap map = null;
	public MapUtils mapUtils = null;
	public Vector2 worldSizeScaledPx = null, worldSizeScaledMt = null, worldSizeTiles = null;
	private ScalingStrategy scalingStrategy;
	private World b2dWorld;

	// player data
	public Vector2 playerStartPos = new Vector2();
	public float playerStartOrient;
	public int playerStartTileX, playerStartTileY;

	// lighting system
	private boolean nightMode;
	protected RayHandler rayHandler = null;
	protected ConeLight playerHeadlights = null;

	// level meshes, package-level access for GameWorldRenderer (ugly but faster than accessors)
	protected TrackWalls trackWalls = null;
	protected TrackTrees trackTrees = null;
	protected List<OrthographicAlignedStillModel> staticMeshes = new ArrayList<OrthographicAlignedStillModel>();

	public GameWorld( World b2dWorld, ScalingStrategy strategy, String levelName, boolean nightMode ) {
		scalingStrategy = strategy;
		this.b2dWorld = b2dWorld;
		this.name = levelName;
		this.nightMode = nightMode;

		// ie. "level1-128.tmx"
		String mapname = levelName + "-" + (int)scalingStrategy.forTileSize + ".tmx";
		FileHandle mapHandle = Gdx.files.internal( Config.LevelsStore + mapname );

		// load tilemap
		map = TiledLoader.createMap( mapHandle );

		// compute world size
		worldSizeTiles = new Vector2( map.width, map.height );
		worldSizeScaledPx = new Vector2( map.width * map.tileWidth, map.height * map.tileHeight );
		worldSizeScaledPx.mul( scalingStrategy.invTileMapZoomFactor );
		worldSizeScaledMt = new Vector2( Convert.px2mt( worldSizeScaledPx ) );

		// initialize tilemap utils
		mapUtils = new MapUtils( map, worldSizeScaledPx, scalingStrategy.invTileMapZoomFactor );
		ModelFactory.init( strategy, mapUtils );

		createMeshes();
		loadPlayer( map );

		// FIXME, read night mode from level?
		if( nightMode ) {
			createLights();
		}
	}

	public void dispose() {
		// dispose any static mesh previously loaded
		for( int i = 0; i < staticMeshes.size(); i++ ) {
			OrthographicAlignedStillModel model = staticMeshes.get( i );
			model.dispose();
		}

		trackWalls.dispose();
		trackTrees.dispose();
	}

	private void createMeshes() {
		staticMeshes.clear();
		TotalMeshes = 0;

		// static meshes layer
		if( mapUtils.hasObjectGroup( MapUtils.LayerStaticMeshes ) ) {
			TiledObjectGroup group = mapUtils.getObjectGroup( MapUtils.LayerStaticMeshes );
			for( int i = 0; i < group.objects.size(); i++ ) {
				TiledObject o = group.objects.get( i );

				float scale = 1f;
				if( o.properties.get( MapUtils.MeshScale ) != null ) {
					scale = Float.parseFloat( o.properties.get( MapUtils.MeshScale ) );
				}

				OrthographicAlignedStillModel mesh = ModelFactory.create( o.type, o.x, o.y, scale );
				if( mesh != null ) {
					staticMeshes.add( mesh );
				}
			}
		}

		// walls by polylines
		List<OrthographicAlignedStillModel> walls = createWalls();
		trackWalls = new TrackWalls( walls, true );

		// trees
		List<TreeStillModel> trees = createTrees();
		trackTrees = new TrackTrees( trees, true );

		TotalMeshes = staticMeshes.size() + trackWalls.count() + trackTrees.count();
	}

	private void loadPlayer( TiledMap map ) {
		// search the map for the start marker and create
		// the player with the found tile coordinates
		float halfTile = map.tileWidth / 2;

		TiledLayer layerTrack = mapUtils.getLayer( MapUtils.LayerTrack );
		Vector2 start = new Vector2();
		int startTileX = 0, startTileY = 0;

		for( int y = 0; y < map.height; y++ ) {
			for( int x = 0; x < map.width; x++ ) {
				int id = layerTrack.tiles[y][x];
				String type = map.getTileProperty( id, "type" );
				if( type == null ) {
					continue;
				}

				if( type.equals( "start" ) ) {
					start.set( mapUtils.tileToPx( x, y ).add( Convert.scaledPixels( halfTile, -halfTile ) ) );
					startTileX = x;
					startTileY = y;
					break;
				}
			}
		}

		String direction = layerTrack.properties.get( "start" );
		float startOrient = mapUtils.orientationFromDirection( direction );

		// set player data
		playerStartOrient = startOrient;
		playerStartPos.set( start );
		playerStartTileX = startTileX;
		playerStartTileY = startTileY;
	}

	private void createLights() {
		if( !mapUtils.hasObjectGroup( MapUtils.LayerLights ) ) {
			this.nightMode = false;
			return;
		}

		float rttScale = .5f;
		int maxRays = 720;

		if( !Config.isDesktop ) {
			rttScale = 0.2f;
			maxRays = 360;
		}

		RayHandler.setColorPrecisionMediump();
		rayHandler = new RayHandler( b2dWorld, maxRays, (int)(Gdx.graphics.getWidth() * rttScale), (int)(Gdx.graphics.getHeight() * rttScale) );
		rayHandler.setShadows( true );
		rayHandler.setCulling( true );
		rayHandler.setBlur( true );
		rayHandler.setBlurNum( 1 );
		rayHandler.setAmbientLight( 0f, 0, 0.25f, 0.2f );

		// attach light to player
		final Color c = new Color();

		// setup player headlights
		c.set( .4f, .4f, .75f, .85f );
		playerHeadlights = new ConeLight( rayHandler, maxRays, c, 30, 0, 0, 0, 15 );
		playerHeadlights.setSoft( false );
		playerHeadlights.setMaskBits( CollisionFilters.CategoryTrackWalls );

		Vector2 pos = new Vector2();
		TiledObjectGroup group = mapUtils.getObjectGroup( MapUtils.LayerLights );
		for( int i = 0; i < group.objects.size(); i++ ) {
			c.set(
			// MathUtils.random(0,1),
			// MathUtils.random(0,1),
			// MathUtils.random(0,1),
			1f, .85f, .15f, .75f );
			TiledObject o = group.objects.get( i );
			pos.set( o.x, o.y ).mul( scalingStrategy.invTileMapZoomFactor );
			pos.y = worldSizeScaledPx.y - pos.y;
			pos.set( Convert.px2mt( pos ) );

			PointLight l = new PointLight( rayHandler, maxRays, c, 30f, pos.x, pos.y );
			l.setSoft( false );
			l.setMaskBits( CollisionFilters.CategoryPlayer | CollisionFilters.CategoryTrackWalls );
		}
	}

	//
	// construct walls
	//
	public List<OrthographicAlignedStillModel> createWalls() {
		List<OrthographicAlignedStillModel> models = null;

		if( mapUtils.hasObjectGroup( MapUtils.LayerWalls ) ) {
			Vector2 fromMt = new Vector2();
			Vector2 toMt = new Vector2();
			Vector2 offsetMt = new Vector2();

			// create material
			TextureAttribute ta = new TextureAttribute( Art.meshTrackWall, 0, "textureAttributes" );
			ta.uWrap = TextureWrap.Repeat.getGLEnum();
			ta.vWrap = TextureWrap.Repeat.getGLEnum();
			Material mat = new Material( "trackWall", ta );

			TiledObjectGroup group = mapUtils.getObjectGroup( MapUtils.LayerWalls );
			if( group.objects.size() > 0 ) {
				models = new ArrayList<OrthographicAlignedStillModel>( group.objects.size() );

				for( int i = 0; i < group.objects.size(); i++ ) {
					TiledObject o = group.objects.get( i );

					List<Vector2> points = MapUtils.extractPolyData( o.polyline );
					if( points.size() >= 2 ) {
						float factor = GameData.Environment.scalingStrategy.invTileMapZoomFactor;
						float wallSizeMt = 0.3f * factor;
						float[] mags = new float[ points.size() - 1 ];

						offsetMt.set( o.x, o.y );
						offsetMt.set( Convert.px2mt( offsetMt ) );

						fromMt.set( Convert.px2mt( points.get( 0 ) ) ).add( offsetMt ).mul( factor );
						fromMt.y = worldSizeScaledMt.y - fromMt.y;

						for( int j = 1; j <= points.size() - 1; j++ ) {
							toMt.set( Convert.px2mt( points.get( j ) ) ).add( offsetMt ).mul( factor );
							toMt.y = worldSizeScaledMt.y - toMt.y;

							// create box2d wall
							Box2DFactory.createWall( GameData.Environment.b2dWorld, fromMt, toMt, wallSizeMt, 0f );

							// compute magnitude
							mags[j - 1] = (float)Math.sqrt( (toMt.x - fromMt.x) * (toMt.x - fromMt.x) + (toMt.y - fromMt.y) * (toMt.y - fromMt.y) );

							fromMt.set( toMt );
						}

						Mesh mesh = buildWallMesh( points, mags );

						StillSubMesh[] subMeshes = new StillSubMesh[ 1 ];
						subMeshes[0] = new StillSubMesh( "wall", mesh, GL10.GL_TRIANGLES );

						OrthographicAlignedStillModel model = new OrthographicAlignedStillModel( mapUtils, new StillModel( subMeshes ), mat,
								GameData.Environment.scalingStrategy );

						model.setPosition( o.x, o.y );
						model.setScale( 1 );

						models.add( model );
					}
				}
			}
		}

		return models;
	}

	private Mesh buildWallMesh( List<Vector2> points, float[] magnitudes ) {
		final int X1 = 0;
		final int Y1 = 1;
		final int Z1 = 2;
		final int U1 = 3;
		final int V1 = 4;
		final int X2 = 5;
		final int Y2 = 6;
		final int Z2 = 7;
		final int U2 = 8;
		final int V2 = 9;

		Vector2 in = new Vector2();
		MathUtils.random.setSeed( Long.MIN_VALUE );

		// scaling factors
		float factor = GameData.Environment.scalingStrategy.invTileMapZoomFactor;
		float oneOnWorld3DFactor = 1f / OrthographicAlignedStillModel.World3DScalingFactor;
		float wallHeightMt = 5f * factor * oneOnWorld3DFactor;
		float textureScalingU = 1f;
		float coordU = 1f;
		float coordV = 3f;

		// jitter
		float jitterPositional = .5f * factor * oneOnWorld3DFactor;
		// float jitterAltitudinal = 3f * factor * oneOnWorld3DFactor;
		boolean addJitter = true;

		int vertexCount = points.size() * 2;
		int indexCount = (points.size() - 1) * 6;

		int vertSize = 5;	// x, y, z, u, v
		float[] verts = new float[ vertSize * vertexCount ];
		short[] indices = new short[ indexCount ];
		float mag, prevmag;
		mag = magnitudes[0];
		prevmag = magnitudes[0];

		// add input (interleaved w/ later filled dupes w/ just a meaningful z-coordinate)
		for( int i = 0, j = 0, vc = 0, vci = 0; i < points.size(); i++, j += 2 * vertSize ) {
			int magidx = i - 1;
			if( magidx < 0 ) {
				magidx = 0;
			}

			mag = AMath.lerp( prevmag, magnitudes[magidx], .25f );
			prevmag = mag;

			coordU = mag * textureScalingU;

			in.set( Convert.px2mt( points.get( i ) ) ).mul( factor * oneOnWorld3DFactor );

			// base
			verts[j + X1] = in.x;
			verts[j + Y1] = -in.y;
			verts[j + Z1] = 0;

			// elevation
			verts[j + X2] = in.x + (addJitter ? MathUtils.random( -jitterPositional, jitterPositional ) : 0);
			verts[j + Y2] = -in.y + (addJitter ? MathUtils.random( -jitterPositional, jitterPositional ) : 0);
			verts[j + Z2] = wallHeightMt;// + (addJitter? MathUtils.random( -jitterAltitudinal, jitterAltitudinal ) :
											// 0);

			// tex coords
			verts[j + U1] = ((i & 1) == 0 ? coordU : 0f);
			verts[j + V1] = coordV;

			verts[j + U2] = ((i & 1) == 0 ? coordU : 0f);
			verts[j + V2] = 0f;

			vc += 2;

			if( vc > 2 ) {
				indices[vci++] = (short)(vc - 3);
				indices[vci++] = (short)(vc - 4);
				indices[vci++] = (short)(vc - 2);
				indices[vci++] = (short)(vc - 3);
				indices[vci++] = (short)(vc - 2);
				indices[vci++] = (short)(vc - 1);
			}
		}

		Mesh mesh = new Mesh( true, vertexCount, indexCount, new VertexAttribute( Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE ), new VertexAttribute(
				Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0" ) );

		mesh.setVertices( verts );
		mesh.setIndices( indices );

		return mesh;
	}

	//
	// construct trees
	//
	private float[] treeRotations = new float[ 4 ];

	private List<TreeStillModel> createTrees() {
		List<TreeStillModel> models = null;

		if( mapUtils.hasObjectGroup( MapUtils.LayerTrees ) ) {

			// We want to differentiate tree meshes as much as we can
			// rotation will helps immensely, but non-orthogonal rotations
			// will cause the bounding box to get recomputed only approximately
			// thus loosing precision: orthogonal rotations instead provides high
			// quality AABB recomputation.
			//
			// We still have 4 variations for any given tree!
			treeRotations[0] = 0;
			treeRotations[1] = 90;
			treeRotations[2] = 180;
			treeRotations[3] = 270;

			MathUtils.random.setSeed( Long.MAX_VALUE );
			TiledObjectGroup group = mapUtils.getObjectGroup( MapUtils.LayerTrees );

			if( group.objects.size() > 0 ) {

				models = new ArrayList<TreeStillModel>( group.objects.size() );

				for( int i = 0; i < group.objects.size(); i++ ) {
					TiledObject o = group.objects.get( i );

					float scale = 1f;
					if( o.properties.get( MapUtils.MeshScale ) != null ) {
						scale = Float.parseFloat( o.properties.get( MapUtils.MeshScale ) );
					}

					TreeStillModel model = null;
					if( o.type != null ) {
						model = ModelFactory.createTree( o.type, o.x, o.y, scale );
					} else {
						Gdx.app.log( "TrackTrees", "Load error, no type was given for the tree #" + (i + 1) );
					}

					if( model != null ) {
						// model.setRotation( MathUtils.random( -180f, 180f ), 0, 0, 1f );
						model.setRotation( treeRotations[MathUtils.random( 0, 3 )], 0, 0, 1f );
						models.add( nextIndexForTrees( models, model ), model );
					}
				}
			}
		}

		return models;
	}

	private int nextIndexForTrees( List<TreeStillModel> models, TreeStillModel model ) {
		for( int i = 0; i < models.size(); i++ ) {
			if( model.material.equals( models.get( i ).material ) ) {
				return i;
			}
		}

		return 0;
	}

	public boolean isNightMode() {
		return nightMode;
	}

	public final TrackWalls getTrackWalls() {
		return trackWalls;
	}

	public final TrackTrees getTrackTrees() {
		return trackTrees;
	}

	public final List<OrthographicAlignedStillModel> getStaticMeshes() {
		return staticMeshes;
	}

	public final RayHandler getRayHandler() {
		return rayHandler;
	}

	public final ConeLight getPlayerHeadLights() {
		return playerHeadlights;
	}
}
