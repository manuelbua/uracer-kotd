package com.bitfire.uracer.game.world;

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
import com.badlogic.gdx.graphics.Mesh.VertexDataType;
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
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.collisions.CollisionFilters;
import com.bitfire.uracer.game.collisions.GameWorldContactListener;
import com.bitfire.uracer.game.world.models.MapUtils;
import com.bitfire.uracer.game.world.models.ModelFactory;
import com.bitfire.uracer.game.world.models.OrthographicAlignedStillModel;
import com.bitfire.uracer.game.world.models.TrackTrees;
import com.bitfire.uracer.game.world.models.TrackWalls;
import com.bitfire.uracer.game.world.models.TreeStillModel;
import com.bitfire.uracer.game.world.models.WorldDefs.LayerProperties;
import com.bitfire.uracer.game.world.models.WorldDefs.ObjectGroup;
import com.bitfire.uracer.game.world.models.WorldDefs.ObjectProperties;
import com.bitfire.uracer.game.world.models.WorldDefs.TileLayer;
import com.bitfire.uracer.game.world.models.WorldDefs.TileProperties;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.Convert;

/** Encapsulates the game's world. Yay!
 *
 * @author bmanuel */
public final class GameWorld {

	// statistics
	public static int TotalMeshes = 0;

	// public level data
	public String levelName = "no-level-loaded";
	public TiledMap map = null;
	public Vector2 worldSizeScaledPx = null, worldSizeScaledMt = null, worldSizeTiles = null;
	public ScalingStrategy scalingStrategy;

	// private level data
	private World box2dWorld;
	private MapUtils mapUtils = null;

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

	public GameWorld( ScalingStrategy strategy, String levelName, boolean nightMode ) {
		scalingStrategy = strategy;
		this.box2dWorld = new World( new Vector2( 0, 0 ), false );
		box2dWorld.setContactListener( new GameWorldContactListener() );
		Gdx.app.log( "GameWorld", "Box2D world created" );

		this.levelName = levelName;
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
		ModelFactory.init( strategy );

		createMeshes();
		loadPlayerData( map );

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
		box2dWorld.dispose();
	}

	private void createMeshes() {
		staticMeshes.clear();
		TotalMeshes = 0;

		// static meshes layer
		if( mapUtils.hasObjectGroup( ObjectGroup.StaticMeshes ) ) {
			TiledObjectGroup group = mapUtils.getObjectGroup( ObjectGroup.StaticMeshes );
			for( int i = 0; i < group.objects.size(); i++ ) {
				TiledObject o = group.objects.get( i );

				float scale = 1f;
				if( o.properties.get( ObjectProperties.MeshScale.mnemonic ) != null ) {
					scale = Float.parseFloat( o.properties.get( ObjectProperties.MeshScale.mnemonic ) );
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
		trackTrees = new TrackTrees( mapUtils, trees, true );

		TotalMeshes = staticMeshes.size() + trackWalls.count() + trackTrees.count();
	}

	private void loadPlayerData( TiledMap map ) {
		// search the map for the start marker and create
		// the player with the found tile coordinates
		float halfTile = map.tileWidth / 2;

		TiledLayer layerTrack = mapUtils.getLayer( TileLayer.Track );
		Vector2 start = new Vector2();
		int startTileX = 0, startTileY = 0;

		for( int y = 0; y < map.height; y++ ) {
			for( int x = 0; x < map.width; x++ ) {
				int id = layerTrack.tiles[y][x];
				String type = map.getTileProperty( id, TileProperties.Type.mnemonic );
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

		String direction = layerTrack.properties.get( LayerProperties.Start.mnemonic );
		float startOrient = mapUtils.orientationFromDirection( direction );

		// set player data
		playerStartOrient = startOrient;
		playerStartPos.set( start );
		playerStartTileX = startTileX;
		playerStartTileY = startTileY;
	}

	private void createLights() {
		if( !mapUtils.hasObjectGroup( ObjectGroup.Lights ) ) {
			this.nightMode = false;
			return;
		}

		float rttScale = .25f;
		int maxRays = 360;

		if( !Config.isDesktop ) {
			rttScale = 0.2f;
			maxRays = 360;
		}

		RayHandler.setColorPrecisionMediump();
		rayHandler = new RayHandler( box2dWorld, maxRays, (int)(Gdx.graphics.getWidth() * rttScale), (int)(Gdx.graphics.getHeight() * rttScale) );
		rayHandler.setShadows( true );
		rayHandler.setCulling( true );
		rayHandler.setBlur( true );
		rayHandler.setBlurNum( 1 );
		rayHandler.setAmbientLight( 0f, 0, 0.25f, 0.2f );

		final Color c = new Color();

		// setup player headlights data
		c.set( .4f, .4f, .75f, .85f );
		playerHeadlights = new ConeLight( rayHandler, maxRays, c, 30, 0, 0, 0, 15 );
		playerHeadlights.setSoft( false );
		playerHeadlights.setMaskBits( CollisionFilters.CategoryTrackWalls );

		// setup level lights data, if any
		Vector2 pos = new Vector2();
		TiledObjectGroup group = mapUtils.getObjectGroup( ObjectGroup.Lights );
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

			PointLight l = new PointLight( rayHandler, maxRays, c, 10f, pos.x, pos.y );
			l.setSoft( false );
			l.setMaskBits( CollisionFilters.CategoryPlayer | CollisionFilters.CategoryTrackWalls );
		}
	}

	//
	// construct walls
	//

	public List<OrthographicAlignedStillModel> createWalls() {
		List<OrthographicAlignedStillModel> models = null;

		if( mapUtils.hasObjectGroup( ObjectGroup.Walls ) ) {
			Vector2 fromMt = new Vector2();
			Vector2 toMt = new Vector2();
			Vector2 offsetMt = new Vector2();

			// create material
			TextureAttribute ta = new TextureAttribute( Art.meshTrackWall, 0, "textureAttributes" );
			ta.uWrap = TextureWrap.Repeat.getGLEnum();
			ta.vWrap = TextureWrap.Repeat.getGLEnum();
			Material mat = new Material( "trackWall", ta );

			TiledObjectGroup group = mapUtils.getObjectGroup( ObjectGroup.Walls );
			if( group.objects.size() > 0 ) {
				models = new ArrayList<OrthographicAlignedStillModel>( group.objects.size() );

				for( int i = 0; i < group.objects.size(); i++ ) {
					TiledObject o = group.objects.get( i );

					List<Vector2> points = MapUtils.extractPolyData( o.polyline );
					if( points.size() >= 2 ) {
						float factor = scalingStrategy.invTileMapZoomFactor;
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
							Box2DFactory.createWall( box2dWorld, fromMt, toMt, wallSizeMt, 0f );

							// compute magnitude
							mags[j - 1] = (float)Math.sqrt( (toMt.x - fromMt.x) * (toMt.x - fromMt.x) + (toMt.y - fromMt.y) * (toMt.y - fromMt.y) );

							fromMt.set( toMt );
						}

						Mesh mesh = buildWallMesh( points, mags );

						StillSubMesh[] subMeshes = new StillSubMesh[ 1 ];
						subMeshes[0] = new StillSubMesh( "wall", mesh, GL10.GL_TRIANGLES );

						OrthographicAlignedStillModel model = new OrthographicAlignedStillModel( new StillModel( subMeshes ), mat, scalingStrategy );

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
		float factor = scalingStrategy.invTileMapZoomFactor;
		float oneOnWorld3DFactor = 1f / OrthographicAlignedStillModel.World3DScalingFactor;
		float wallHeightMt = 5f * factor * oneOnWorld3DFactor;
		float textureScalingU = 0.5f;
		float coordU = 1f;
		float coordV = 1f;

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

			mag = AMath.lerp( prevmag, magnitudes[magidx], .5f );
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

		Mesh mesh = new Mesh( VertexDataType.VertexArray, true, vertexCount, indexCount, new VertexAttribute( Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE ),
				new VertexAttribute( Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0" ) );

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

		if( mapUtils.hasObjectGroup( ObjectGroup.Trees ) ) {

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
			TiledObjectGroup group = mapUtils.getObjectGroup( ObjectGroup.Trees );

			if( group.objects.size() > 0 ) {

				models = new ArrayList<TreeStillModel>( group.objects.size() );

				for( int i = 0; i < group.objects.size(); i++ ) {
					TiledObject o = group.objects.get( i );

					float scale = 1f;
					if( o.properties.get( ObjectProperties.MeshScale.mnemonic ) != null ) {
						scale = Float.parseFloat( o.properties.get( ObjectProperties.MeshScale.mnemonic ) );
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

	public TrackWalls getTrackWalls() {
		return trackWalls;
	}

	public TrackTrees getTrackTrees() {
		return trackTrees;
	}

	public List<OrthographicAlignedStillModel> getStaticMeshes() {
		return staticMeshes;
	}

	public RayHandler getRayHandler() {
		return rayHandler;
	}

	public ConeLight getPlayerHeadLights() {
		return playerHeadlights;
	}

	public World getBox2DWorld() {
		return box2dWorld;
	}

	// helpers from maputils

	public Vector2 positionFor( Vector2 position ) {
		return mapUtils.positionFor( position );
	}

	public Vector2 positionFor( float x, float y ) {
		return mapUtils.positionFor( x, y );
	}

	public Vector2 pxToTile( float x, float y ) {
		return mapUtils.pxToTile( x, y );
	}

	public float getTileSizeScaled() {
		return mapUtils.scaledTilesize;
	}

	public float getTileSizeInvScaled() {
		return mapUtils.invScaledTilesize;
	}

	public TiledLayer getLayer( TileLayer layer ) {
		return mapUtils.getLayer( layer );
	}

	public boolean isValidTilePosition( Vector2 tilePosition ) {
		return tilePosition.x >= 0 && tilePosition.x < map.width && tilePosition.y >= 0 && tilePosition.y < map.height;
	}
}
