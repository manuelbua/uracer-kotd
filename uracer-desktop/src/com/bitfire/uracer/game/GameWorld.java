package com.bitfire.uracer.game;

import java.util.ArrayList;

import box2dLight.ConeLight;
import box2dLight.PointLight;
import box2dLight.RayHandler;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.tiled.TiledLayer;
import com.badlogic.gdx.graphics.g2d.tiled.TiledLoader;
import com.badlogic.gdx.graphics.g2d.tiled.TiledMap;
import com.badlogic.gdx.graphics.g2d.tiled.TiledObject;
import com.badlogic.gdx.graphics.g2d.tiled.TiledObjectGroup;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.entities.CollisionFilters;
import com.bitfire.uracer.factories.ModelFactory;
import com.bitfire.uracer.tiled.OrthographicAlignedStillModel;
import com.bitfire.uracer.tiled.TrackTrees;
import com.bitfire.uracer.tiled.TrackWalls;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.MapUtils;

/** First write. Basic idea in place (in iterative refactoring)
 * FIXME should go with something else instead, eg. MVC/no Level..
 * @author manuel */
public class GameWorld {
	// level data
	public String name = "no-level-loaded";
	public TiledMap map = null;
	public Vector2 worldSizeScaledPx = null, worldSizeScaledMt = null, worldSizeTiles = null;

	// player data
	public Vector2 playerStartPos = new Vector2();
	public float playerStartOrient;
	public int playerStartTileX, playerStartTileY;

	// lighting system
	private boolean nightMode;
	protected RayHandler rayHandler = null;
	protected ConeLight playerHeadlights = null;

	// statistics
	public static int TotalMeshes = 0;

	// level meshes, package-level access for GameWorldRenderer (ugly but faster than accessors)
	protected TrackWalls trackWalls = null;
	protected TrackTrees trackTrees = null;
	protected ArrayList<OrthographicAlignedStillModel> staticMeshes = new ArrayList<OrthographicAlignedStillModel>();

	public GameWorld( String levelName, boolean nightMode ) {
		this.name = levelName;
		this.nightMode = nightMode;

		// ie. "level1-128.tmx"
		String mapname = levelName + "-" + (int)GameData.scalingStrategy.forTileSize + ".tmx";
		FileHandle mapHandle = Gdx.files.internal( Config.LevelsStore + mapname );

		// load tilemap
		map = TiledLoader.createMap( mapHandle );

		// compute world size
		worldSizeTiles = new Vector2( map.width, map.height );
		worldSizeScaledPx = new Vector2( map.width * map.tileWidth, map.height * map.tileHeight );
		worldSizeScaledPx.mul( GameData.scalingStrategy.invTileMapZoomFactor );
		worldSizeScaledMt = new Vector2( Convert.px2mt( worldSizeScaledPx ) );

		// initialize tilemap utils
		MapUtils.init( map, worldSizeScaledPx );

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
		trackWalls.createWalls( GameData.b2dWorld, worldSizeScaledMt );

		// trees
		trackTrees = new TrackTrees();
		trackTrees.createTrees();

		TotalMeshes = staticMeshes.size() + trackWalls.walls.size() + trackTrees.trees.size();
	}

	private void loadPlayer( TiledMap map ) {
		// search the map for the start marker and create
		// the player with the found tile coordinates
		float halfTile = map.tileWidth / 2;

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
					start.set( MapUtils.tileToPx( x, y ).add( Convert.scaledPixels( halfTile, -halfTile ) ) );
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

		// set player data
		playerStartOrient = startOrient;
		playerStartPos.set(start);
		playerStartTileX = startTileX;
		playerStartTileY = startTileY;
	}

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
		rayHandler = new RayHandler( GameData.b2dWorld, maxRays, (int)(Gdx.graphics.getWidth() * rttScale), (int)(Gdx.graphics.getHeight() * rttScale) );
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

	public boolean isNightMode() {
		return nightMode;
	}
}
