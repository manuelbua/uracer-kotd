package com.bitfire.uracer.tiled;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.tiled.TileAtlas;
import com.badlogic.gdx.graphics.g2d.tiled.TileMapRenderer;
import com.badlogic.gdx.graphics.g2d.tiled.TiledLayer;
import com.badlogic.gdx.graphics.g2d.tiled.TiledLoader;
import com.badlogic.gdx.graphics.g2d.tiled.TiledMap;
import com.badlogic.gdx.graphics.g2d.tiled.TiledObject;
import com.badlogic.gdx.graphics.g2d.tiled.TiledObjectGroup;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.carsimulation.CarModel;
import com.bitfire.uracer.carsimulation.Recorder;
import com.bitfire.uracer.carsimulation.Replay;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.entities.vehicles.GhostCar;
import com.bitfire.uracer.factories.CarFactory;
import com.bitfire.uracer.factories.CarFactory.CarType;
import com.bitfire.uracer.factories.ModelFactory;
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
		OrthographicAlignedStillModel.initialize();

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

		renderOrthographicAlignedModels( staticMeshes );

		// TODO, either disable Track rendering in release/mobile or
		// make Track build a single mesh out of the whole track,
		// there is no point in wasting draw calls/context switching
		// for every single wall tile (!)
		//
		// HUGE performance hit enabling rendering of tile-based walls
		// on mobile (Tegra2)

		if( Config.Graphics.RenderTrackMeshes && track.hasMeshes() )
		{
			gl.glEnable( GL20.GL_BLEND );
			renderOrthographicAlignedModels( track.getMeshes() );
			gl.glDisable( GL20.GL_BLEND );
		}

		gl.glDisable( GL20.GL_DEPTH_TEST );
		gl.glDisable( GL20.GL_CULL_FACE );
	}

	private Vector3 tmpvec = new Vector3();
	private Matrix4 mtx = new Matrix4();
	private Matrix4 mtx2 = new Matrix4();
	private void renderOrthographicAlignedModels(ArrayList<OrthographicAlignedStillModel> models)
	{
		OrthographicAlignedStillModel m;

		// TODO: precompute
		float halfVpW = camOrtho.viewportWidth / 2;
		float halfVpH = camOrtho.viewportHeight / 2;
		float meshZ = -(camPersp.far - camPersp.position.z);

		ShaderProgram shader = OrthographicAlignedStillModel.shaderProgram;
		shader.begin();

		for( int i = 0; i < models.size(); i++ )
		{
			m = models.get( i );

			// compute position
			tmpvec.x = Convert.scaledPixels( m.positionOffsetPx.x - camOrtho.position.x ) + halfVpW + m.positionPx.x;
			tmpvec.y = Convert.scaledPixels( m.positionOffsetPx.y + camOrtho.position.y ) + halfVpH - m.positionPx.y;
			tmpvec.z = 1;

			// transform to world space
			camPersp.unproject( tmpvec );

			// build model matrix
			// TODO: support proper rotation now that Mat3/Mat4 supports opengl-style rotation/translation/scaling
			mtx.setToTranslation( tmpvec.x, tmpvec.y, meshZ );
			Matrix4.mul( mtx.val, mtx2.setToRotation( m.iRotationAxis, m.iRotationAngle ).val );
			Matrix4.mul( mtx.val, mtx2.setToScaling( m.scaleAxis ).val );

			// comb = (proj * view) * model (fast mul)
			Matrix4.mul( mtx2.set( camPersp.combined ).val, mtx.val );

			shader.setUniformMatrix( "u_mvpMatrix", mtx2 );

			// do not bind/rebind textures without reason
			if( i == 0 )
			{
				m.material.bind(shader);
			} else
			if( !models.get(i - 1).material.equals(m.material) )
			{
				m.material.bind(shader);
			}

			m.model.subMeshes[0].mesh.render(shader, m.model.subMeshes[0].primitiveType);

		}

		shader.end();
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
	}

	private void createEntities()
	{
		player = createPlayer(map);
		ghost = CarFactory.createGhost( player );
	}

	private Car createPlayer(TiledMap map)
	{

		// search the map for the start marker and create
		// the player with the found tile coordinates
		TiledLayer layerTrack = MapUtils.getLayer( MapUtils.LayerTrack );
		String startOrient = layerTrack.properties.get("start");
		for( int y = 0; y < map.height; y++ )
		{
			for( int x = 0; x < map.width; x++ )
			{
				int id = layerTrack.tiles[y][x];
				String type = map.getTileProperty(id, "type");
				if(type == null) continue;

				if(type.equals("start"))
				{
					playerStartPos.set( Convert.tileToPx( x, y ).add( Convert.scaledPixels( 112, -112 ) ) );

					if(startOrient.equals("up"))
						playerStartOrient = 0f;
					else if(startOrient.equals("right"))
						playerStartOrient = 90f;
					else if(startOrient.equals("down"))
						playerStartOrient = 180f;
					else if(startOrient.equals("left"))
						playerStartOrient = 270f;

					break;
				}

			}
		}

		return CarFactory.createPlayer( CarType.OldSkool, new CarModel().toModel2(), playerStartPos, playerStartOrient );
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
