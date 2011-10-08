package com.bitfire.uracer.tiled;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.tiled.TileAtlas;
import com.badlogic.gdx.graphics.g2d.tiled.TileMapRenderer;
import com.badlogic.gdx.graphics.g2d.tiled.TiledLoader;
import com.badlogic.gdx.graphics.g2d.tiled.TiledMap;
import com.badlogic.gdx.graphics.g2d.tiled.TiledObject;
import com.badlogic.gdx.graphics.g2d.tiled.TiledObjectGroup;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.entities.vehicles.GhostCar;
import com.bitfire.uracer.factories.CarFactory;
import com.bitfire.uracer.factories.CarFactory.CarType;
import com.bitfire.uracer.factories.ModelFactory;
import com.bitfire.uracer.simulations.car.CarModel;
import com.bitfire.uracer.simulations.car.Recorder;
import com.bitfire.uracer.simulations.car.Replay;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.MapUtils;

/**
 * First write. Basic idea in place, will need refactoring for sure.
 *
 * @author manuel
 *
 */
public class Level
{
	public TiledMap map = null;
	public TileMapRenderer renderer = null;
	public Track track = null;
	public String name = "";

	private static final String LevelsStore = "data/levels/";
	private TileAtlas atlas = null;
	private PerspectiveCamera camPersp = null;
	private OrthographicCamera camOrtho = null;
	private float camPerspElevation = 0f;
	private ArrayList<OrthographicAlignedStillModel> staticMeshes = new ArrayList<OrthographicAlignedStillModel>();

	// game data/logic
	private Car player;
	private GhostCar ghost;
	private Recorder recorder;

	private Vector2 playerStartPos = new Vector2();
	private float playerStartOrient = 0f;

	public Level( String levelName, ScalingStrategy strategy )
	{
		this.name = levelName;

		// ie. "level1-128.tmx"
		String mapname = levelName + "-" + (int)strategy.forTileSize + ".tmx";
		FileHandle mapHandle = Gdx.files.internal( LevelsStore + mapname );
		FileHandle baseDir = Gdx.files.internal( LevelsStore );

		// load tilemap
		map = TiledLoader.createMap( mapHandle );
		atlas = new TileAtlas( map, baseDir );
		renderer = new TileMapRenderer( map, atlas, 1, 1, map.tileWidth, map.tileHeight );

		// initialize TiledMap utils
		MapUtils.initialize( map );

		createCams();
	}

	public void construct()
	{
		syncWithCam( Director.getCamera() );
		OrthographicAlignedStillModel.initialize( camOrtho, camPersp );

		ModelFactory.init();

		// create track
		track = new Track( map );

		EntityManager.create();
		recorder = Recorder.create();

		createMeshes();
		createEntities();
	}

	public void dispose()
	{
		// clear references to track meshes
		if( track != null && track.hasMeshes() )
		{
			track.getMeshes().clear();
		}

		// dispose any static mesh previously loaded
		for( int i = 0; i < staticMeshes.size(); i++ )
		{
			OrthographicAlignedStillModel model = staticMeshes.get( i );
			model.dispose();
		}

	}

	public void syncWithCam( OrthographicCamera orthoCam )
	{
		// scale position
		camOrtho.position.set( orthoCam.position );
		camOrtho.position.mul( Director.scalingStrategy.tileMapZoomFactor );

		camOrtho.viewportWidth = Gdx.graphics.getWidth();
		camOrtho.viewportHeight = Gdx.graphics.getHeight();
		camOrtho.zoom = Director.scalingStrategy.tileMapZoomFactor;
		camOrtho.update();

		camPersp.viewportWidth = camOrtho.viewportWidth;
		camPersp.viewportHeight = camOrtho.viewportHeight;
		camPersp.position.set( camOrtho.position.x, camOrtho.position.y, camPerspElevation );
		camPersp.fieldOfView = Director.scalingStrategy.verticalFov;
		camPersp.update();
	}

	public void renderTilemap()
	{
		renderer.render( camOrtho );
	}

	public void renderMeshes( GL20 gl )
	{
		gl.glEnable( GL20.GL_CULL_FACE );
		gl.glEnable( GL20.GL_DEPTH_TEST );
		gl.glDepthFunc( GL20.GL_LESS );

		OrthographicAlignedStillModel m;

		// render object group meshes
		for( int i = 0; i < staticMeshes.size(); i++ )
		{
			m = staticMeshes.get( i );
			m.render( gl );
		}

		if( track.hasMeshes() )
		{
			track.render( gl );
		}

		gl.glDisable( GL20.GL_DEPTH_TEST );
		gl.glDisable( GL20.GL_CULL_FACE );
	}

	private void createCams()
	{
		int w = Gdx.graphics.getWidth();
		int h = Gdx.graphics.getHeight();

		// creates and setup orthographic camera
		camOrtho = new OrthographicCamera( w, h );
		camOrtho.near = 0;
		camOrtho.far = 100;
		camOrtho.zoom = 1;

		// creates and setup perspective camera
		float perspPlaneNear = 1;

		// carefully choosen, Blender models' 14.2 meters <=> one 256px tile
		// with far plane @48
		float perspPlaneFar = 240;
		camPerspElevation = 100;

		camPersp = new PerspectiveCamera( Director.scalingStrategy.verticalFov, w, h );
		camPersp.near = perspPlaneNear;
		camPersp.far = perspPlaneFar;
		camPersp.lookAt( 0, 0, -1 );
		camPersp.position.set( 0, 0, camPerspElevation );
	}

	private void createMeshes()
	{
		staticMeshes.clear();

		//
		// create static meshes from level descriptor
		//

		// static meshes layer
		if( MapUtils.hasObjectGroup( MapUtils.LayerStaticMeshes ) )
		{
			TiledObjectGroup group = MapUtils.getObjectGroup( MapUtils.LayerStaticMeshes );
			for( int i = 0; i < group.objects.size(); i++ )
			{
				TiledObject o = group.objects.get( i );

				float scale = 1f;
				if( o.properties.get( MapUtils.MeshScale ) != null )
					scale = Float.parseFloat( o.properties.get( MapUtils.MeshScale ) );

				// System.out.println("Creating " + o.type + ", [" + o.x + "," +
				// o.y
				// + "] x" + scale);
				OrthographicAlignedStillModel mesh = ModelFactory.create( o.type, o.x, o.y, scale );
				if( mesh != null ) staticMeshes.add( mesh );
			}
		}

		// track meshes
		// if( track.hasMeshes() )
		// {
		// ArrayList<OrthographicAlignedStillModel> trackMeshes = track.getMeshes();
		// for( int i = 0; i < trackMeshes.size(); i++ )
		// {
		// staticMeshes.add( trackMeshes.get( i ) );
		// }
		// }
	}

	private void createEntities()
	{
		// TODO: read positions from tmx
		// playerStartPos.set( Convert.tileToPx( 4, 4 ).add( Convert.scaledPixels( 112, -112 ) ) );
		playerStartPos.set( Convert.tileToPx( 2, 1 ).add( Convert.scaledPixels( 112, -112 ) ) );
		playerStartOrient = 90f;

		CarModel m = new CarModel();
		player = CarFactory.createPlayer( CarType.OldSkool, m.toModel2(), playerStartPos, playerStartOrient );
		ghost = CarFactory.createGhost( player );
	}

	/**
	 * Game / game logic
	 */

	public Car getPlayer()
	{
		return player;
	}

	public GhostCar getGhost()
	{
		return ghost;
	}

	public void beginRecording( Replay outputBuffer, long lapStartTimeNs )
	{
		recorder.beginRecording( player, outputBuffer, lapStartTimeNs );
	}

	public void endRecording()
	{
		recorder.endRecording();
	}

	public boolean isRecording()
	{
		return recorder.isRecording();
	}

	public void discardRecording()
	{
		recorder.reset();
	}

	public void restart()
	{
		player.reset();
		ghost.reset();
		recorder.reset();
	}
}
