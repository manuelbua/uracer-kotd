package com.bitfire.uracer.game.logic;

import java.util.ArrayList;

import box2dLight.ConeLight;
import box2dLight.PointLight;
import box2dLight.RayHandler;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.tiled.TileAtlas;
import com.badlogic.gdx.graphics.g2d.tiled.TiledLayer;
import com.badlogic.gdx.graphics.g2d.tiled.TiledLoader;
import com.badlogic.gdx.graphics.g2d.tiled.TiledMap;
import com.badlogic.gdx.graphics.g2d.tiled.TiledObject;
import com.badlogic.gdx.graphics.g2d.tiled.TiledObjectGroup;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.carsimulation.CarModel;
import com.bitfire.uracer.entities.CollisionFilters;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.entities.vehicles.GhostCar;
import com.bitfire.uracer.factories.CarFactory;
import com.bitfire.uracer.factories.CarFactory.CarType;
import com.bitfire.uracer.factories.ModelFactory;
import com.bitfire.uracer.game.GameData;
import com.bitfire.uracer.tiled.LevelRenderer;
import com.bitfire.uracer.tiled.OrthographicAlignedStillModel;
import com.bitfire.uracer.tiled.TrackTrees;
import com.bitfire.uracer.tiled.TrackWalls;
import com.bitfire.uracer.tiled.UTileMapRenderer;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.MapUtils;

/** First write. Basic idea in place (in iterative refactoring)
 * FIXME should go with something else instead, eg. MVC/no Level..
 * @author manuel */
public class LevelLoader {
	private final World world;

	// level data
	public TiledMap map = null;
	private TrackWalls trackWalls = null;
	private TrackTrees trackTrees = null;
	public Vector2 worldSizeScaledPx = null, worldSizeScaledMt = null, worldSizeTiles = null;

	// level rendering
	public LevelRenderer levelRenderer = null;
	public UTileMapRenderer tileMapRenderer = null;
	public String name = "";
	public static int totalMeshes = 0;

	private static final String LevelsStore = "data/levels/";
	private TileAtlas atlas = null;
	private PerspectiveCamera camPersp = null;
	private OrthographicCamera camOrtho = null;
	private float camPerspElevation = 0f;
	private ArrayList<OrthographicAlignedStillModel> staticMeshes = new ArrayList<OrthographicAlignedStillModel>();

	// player recording data
	private PlayerState player;
	private boolean nightMode;

	// lighting system
	private RayHandler rayHandler = null;
	private ConeLight playerHeadlights = null;

	public LevelLoader( World world, String levelName, boolean nightMode, int width, int height ) {
		this.name = levelName;
		this.nightMode = nightMode;
		this.world = world;

		createCams( width, height );

		// ie. "level1-128.tmx"
		String mapname = levelName + "-" + (int)GameData.scalingStrategy.forTileSize + ".tmx";
		FileHandle mapHandle = Gdx.files.internal( LevelsStore + mapname );
		FileHandle baseDir = Gdx.files.internal( LevelsStore );

		// load tilemap
		map = TiledLoader.createMap( mapHandle );
		atlas = new TileAtlas( map, baseDir );
		tileMapRenderer = new UTileMapRenderer( map, atlas, 1, 1, map.tileWidth, map.tileHeight );

		// compute world size
		worldSizeTiles = new Vector2( map.width, map.height );
		worldSizeScaledPx = new Vector2( map.width * map.tileWidth, map.height * map.tileHeight );
		worldSizeScaledPx.mul( GameData.scalingStrategy.invTileMapZoomFactor );
		worldSizeScaledMt = new Vector2( Convert.px2mt( worldSizeScaledPx ) );

		// initialize TiledMap utils
		MapUtils.init( map, worldSizeScaledPx );

		createMeshes();
		player = createPlayer( map );

		if( nightMode ) {
			createLights();
		}

		levelRenderer = new LevelRenderer( camPersp, camOrtho );
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

	public void syncWithCam( OrthographicCamera orthoCam ) {
		// scale position
		camOrtho.position.set( orthoCam.position );
		camOrtho.position.mul( GameData.scalingStrategy.tileMapZoomFactor );

		camOrtho.viewportWidth = Gdx.graphics.getWidth();
		camOrtho.viewportHeight = Gdx.graphics.getHeight();
		camOrtho.zoom = GameData.scalingStrategy.tileMapZoomFactor;
		camOrtho.update();

		camPersp.viewportWidth = camOrtho.viewportWidth;
		camPersp.viewportHeight = camOrtho.viewportHeight;
		camPersp.position.set( camOrtho.position.x, camOrtho.position.y, camPerspElevation );
		camPersp.fieldOfView = GameData.scalingStrategy.verticalFov;
		camPersp.update();
	}

	public void renderTilemap( GL20 gl ) {
		gl.glDisable( GL20.GL_BLEND );
		tileMapRenderer.render( camOrtho );
	}

	public void renderMeshes( GL20 gl ) {
		levelRenderer.resetCounters();

		gl.glDepthMask( true );
		gl.glEnable( GL20.GL_DEPTH_TEST );
		gl.glCullFace( GL20.GL_BACK );
		gl.glFrontFace( GL20.GL_CCW );
		gl.glDepthFunc( GL20.GL_LESS );
		gl.glBlendEquation( GL20.GL_FUNC_ADD );

		levelRenderer.renderWalls( gl, trackWalls );
		levelRenderer.renderTrees( gl, trackTrees );

		// render "static-meshes" layer
		gl.glEnable( GL20.GL_CULL_FACE );
		levelRenderer.renderOrthographicAlignedModels( gl, staticMeshes );

		gl.glDisable( GL20.GL_DEPTH_TEST );
		gl.glDisable( GL20.GL_CULL_FACE );
		gl.glDepthMask( false );
	}

	private void createCams( int width, int height ) {
		// creates and setup orthographic camera
		camOrtho = new OrthographicCamera( width, height );
		camOrtho.near = 0;
		camOrtho.far = 100;
		camOrtho.zoom = 1;

		// creates and setup perspective camera
		float perspPlaneNear = 1;

		// strategically choosen, Blender models' 14.2 meters <=> one 256px tile
		// with far plane @48
		float perspPlaneFar = 240;
		camPerspElevation = 100;

		camPersp = new PerspectiveCamera( GameData.scalingStrategy.verticalFov, width, height );
		camPersp.near = perspPlaneNear;
		camPersp.far = perspPlaneFar;
		camPersp.lookAt( 0, 0, -1 );
		camPersp.position.set( 0, 0, camPerspElevation );
	}

	private void createMeshes() {
		staticMeshes.clear();
		totalMeshes = 0;

		// static meshes layer
		if( MapUtils.hasObjectGroup( MapUtils.LayerStaticMeshes ) ) {
			TiledObjectGroup group = MapUtils.getObjectGroup( MapUtils.LayerStaticMeshes );
			for( int i = 0; i < group.objects.size(); i++ ) {
				TiledObject o = group.objects.get( i );

				float scale = 1f;
				if( o.properties.get( MapUtils.MeshScale ) != null )
					scale = Float.parseFloat( o.properties.get( MapUtils.MeshScale ) );

				OrthographicAlignedStillModel mesh = ModelFactory.create( o.type, o.x, o.y, scale );
				if( mesh != null )
					staticMeshes.add( mesh );
			}
		}

		// walls by polylines
		trackWalls = new TrackWalls();
		trackWalls.createWalls( world, worldSizeScaledMt );

		// trees
		trackTrees = new TrackTrees();
		trackTrees.createTrees();

		totalMeshes = staticMeshes.size() + trackWalls.walls.size() + trackTrees.trees.size();
	}

	private PlayerState createPlayer( TiledMap map ) {
		// search the map for the start marker and create
		// the player with the found tile coordinates
		TiledLayer layerTrack = MapUtils.getLayer( MapUtils.LayerTrack );
		Vector2 start = new Vector2();
		int startTileX = 0, startTileY = 0;

		for( int y = 0; y < map.height; y++ ) {
			for( int x = 0; x < map.width; x++ ) {
				int id = layerTrack.tiles[y][x];
				String type = map.getTileProperty( id, "type" );
				if( type == null )
					continue;

				if( type.equals( "start" ) ) {
					start.set( MapUtils.tileToPx( x, y ).add( Convert.scaledPixels( 112, -112 ) ) );
					startTileX = x;
					startTileY = y;
					break;
				}
			}
		}

		float startOrient = 0f;
		String orient = layerTrack.properties.get( "start" );

		if( orient.equals( "up" ) )
			startOrient = 0f;
		else if( orient.equals( "right" ) )
			startOrient = 90f;
		else if( orient.equals( "down" ) )
			startOrient = 180f;
		else if( orient.equals( "left" ) )
			startOrient = 270f;

		Car car = CarFactory.createPlayer( world, CarType.OldSkool, new CarModel().toModel2(), start, startOrient );
		GhostCar ghost = CarFactory.createGhost( world, car );

		PlayerState p = new PlayerState( car, ghost );
		p.startPos.set( start );
		p.startTileX = startTileX;
		p.startTileY = startTileY;
		p.startOrient = startOrient;

		return p;
	}

	// TODO, renderer stuff?
	private void createLights() {
		if( !MapUtils.hasObjectGroup( MapUtils.LayerLights ) ) {
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
		rayHandler = new RayHandler( world, maxRays, (int)(Gdx.graphics.getWidth() * rttScale), (int)(Gdx.graphics.getHeight() * rttScale) );
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
		TiledObjectGroup group = MapUtils.getObjectGroup( MapUtils.LayerLights );
		for( int i = 0; i < group.objects.size(); i++ ) {
			c.set(
			// MathUtils.random(0,1),
			// MathUtils.random(0,1),
			// MathUtils.random(0,1),
			1f, .85f, .15f, .75f );
			TiledObject o = group.objects.get( i );
			pos.set( o.x, o.y ).mul( GameData.scalingStrategy.invTileMapZoomFactor );
			pos.y = worldSizeScaledPx.y - pos.y;
			pos.set( Convert.px2mt( pos ) );

			PointLight l = new PointLight( rayHandler, maxRays, c, 30f, pos.x, pos.y );
			l.setSoft( false );
			l.setMaskBits( CollisionFilters.CategoryPlayer | CollisionFilters.CategoryTrackWalls );
		}
	}

	public void generateLightMap() {
		// update player light (subframe interpolation ready)
		float ang = 90 + player.car.state().orientation;

		// the body's compound shape should be created with some clever thinking in it :)
		float offx = (player.car.getCarModel().length / 2f) + .25f;
		float offy = 0f;

		float cos = MathUtils.cosDeg( ang );
		float sin = MathUtils.sinDeg( ang );
		float dX = offx * cos - offy * sin;
		float dY = offx * sin + offy * cos;

		float px = Convert.px2mt( player.car.state().position.x ) + dX;
		float py = Convert.px2mt( player.car.state().position.y ) + dY;

		playerHeadlights.setDirection( ang );
		playerHeadlights.setPosition( px, py );

		rayHandler.setCombinedMatrix( Director.getMatViewProjMt(), Convert.px2mt( camOrtho.position.x * GameData.scalingStrategy.invTileMapZoomFactor ),
				Convert.px2mt( camOrtho.position.y * GameData.scalingStrategy.invTileMapZoomFactor ), Convert.px2mt( camOrtho.viewportWidth ),
				Convert.px2mt( camOrtho.viewportHeight ) );

		rayHandler.update();
		rayHandler.generateLightMap();

		// if( Config.isDesktop && (URacer.getFrameCount()&0x1f)==0x1f)
		// {
		// System.out.println("lights rendered="+rayHandler.lightRenderedLastFrame);
		// }
	}

	public void renderLigthMap( FrameBuffer dest ) {
		rayHandler.renderLightMap( dest );
	}

	/** operations */

	public PlayerState getPlayerState() {
		return player;
	}

	public boolean isNightMode() {
		return nightMode;
	}

	public OrthographicCamera getOrthoCamera() {
		return camOrtho;
	}

	public PerspectiveCamera getPerspectiveCamera() {
		return camPersp;
	}

}
