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
import com.badlogic.gdx.graphics.g2d.tiled.TileMapRenderer;
import com.badlogic.gdx.graphics.g2d.tiled.TiledLayer;
import com.badlogic.gdx.graphics.g2d.tiled.TiledLoader;
import com.badlogic.gdx.graphics.g2d.tiled.TiledMap;
import com.badlogic.gdx.graphics.g2d.tiled.TiledObject;
import com.badlogic.gdx.graphics.g2d.tiled.TiledObjectGroup;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.Physics;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.carsimulation.CarModel;
import com.bitfire.uracer.carsimulation.Recorder;
import com.bitfire.uracer.carsimulation.Replay;
import com.bitfire.uracer.entities.CollisionFilters;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.entities.vehicles.GhostCar;
import com.bitfire.uracer.factories.CarFactory;
import com.bitfire.uracer.factories.CarFactory.CarType;
import com.bitfire.uracer.factories.ModelFactory;
import com.bitfire.uracer.tiled.OrthographicAlignedStillModel;
import com.bitfire.uracer.tiled.ScalingStrategy;
import com.bitfire.uracer.tiled.Track;
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

	// game data
	private Player player;
	private Recorder recorder;
	private boolean nightMode;

	// lighting system
	private RayHandler rayHandler = null;
	private ConeLight playerHeadlights = null;

	public Level( String levelName, ScalingStrategy strategy, boolean nightMode )
	{
		this.name = levelName;
		this.nightMode = nightMode;

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

	/* 2-stage construction, avoid <static> problems in Android */
	public void construct()
	{
		syncWithCam( Director.getCamera() );
		OrthographicAlignedStillModel.initialize();

		ModelFactory.init();

		// create track
//		track = new Track( map );

		EntityManager.create();
		recorder = Recorder.create();

		createMeshes();
		player = createPlayer( map );

		if( nightMode )
		{
			createLights();
		}
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

		if( Config.Graphics.RenderTrackMeshes && track != null && track.hasMeshes() )
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

				OrthographicAlignedStillModel mesh = ModelFactory.create( o.type, o.x, o.y, scale );
				if( mesh != null ) staticMeshes.add( mesh );
			}
		}
	}

	private Player createPlayer(TiledMap map )
	{
		// search the map for the start marker and create
		// the player with the found tile coordinates
		TiledLayer layerTrack = MapUtils.getLayer( MapUtils.LayerTrack );
		Vector2 start = new Vector2();
		int startTileX = 0, startTileY = 0;

		for( int y = 0; y < map.height; y++ )
		{
			for( int x = 0; x < map.width; x++ )
			{
				int id = layerTrack.tiles[y][x];
				String type = map.getTileProperty(id, "type");
				if(type == null) continue;

				if(type.equals("start"))
				{
					start.set( Convert.tileToPx(x, y).add(Convert.scaledPixels(112, -112)) );
					startTileX = x;
					startTileY = y;
					break;
				}
			}
		}

		float startOrient = 0f;
		String orient = layerTrack.properties.get("start");
		if(orient.equals("up")) startOrient = 0f;
		else if(orient.equals("right")) startOrient = 90f;
		else if(orient.equals("down")) startOrient = 180f;
		else if(orient.equals("left")) startOrient = 270f;

		Car car = CarFactory.createPlayer( CarType.OldSkool, new CarModel().toModel2(), start, startOrient );
		GhostCar ghost = CarFactory.createGhost( car );

		Player p = new Player(car, ghost);
		p.startPos.set( start );
		p.startTileX = startTileX;
		p.startTileY = startTileY;
		p.startOrient = startOrient;

		return p;
	}

	private void createLights()
	{
		if( !MapUtils.hasObjectGroup( MapUtils.LayerLights ) )
		{
			this.nightMode = false;
			return;
		}

		float rttScale = .5f;
		int maxRays = 720;

		if(!Config.isDesktop)
		{
			rttScale = 0.2f;
			maxRays = 360;
		}

		RayHandler.setColorPrecisionMediump();
		rayHandler = new RayHandler(Physics.world, maxRays, (int)(Gdx.graphics.getWidth()*rttScale), (int)(Gdx.graphics.getHeight()*rttScale));
		rayHandler.setShadows(true);
		rayHandler.setCulling(true);
		rayHandler.setBlur(true);
		rayHandler.setBlurNum(1);
		rayHandler.setAmbientLight( 0, 0.05f, 0.25f, 0.2f );

//		RayHandler.setGammaCorrection( true );
//		RayHandler.useDiffuseLight( true );
//		rayHandler.setAmbientLight( 0, 0.01f, 0.025f, 0f );

		// attach light to player
		final Color c = new Color();

		// setup player headlights
		c.set( .7f, .7f, 1f, .85f );
		playerHeadlights = new ConeLight( rayHandler, maxRays, c, 30, 0, 0, 0, 15 );
		playerHeadlights.setSoft( false );
		playerHeadlights.setMaskBits( 0 );


		Vector2 pos = new Vector2();
		TiledObjectGroup group = MapUtils.getObjectGroup( MapUtils.LayerLights );
		for( int i = 0; i < group.objects.size(); i++ )
		{
			c.set(
					MathUtils.random(0,1),
					MathUtils.random(0,1),
					MathUtils.random(0,1),
					.75f );
			TiledObject o = group.objects.get( i );
			pos.set( o.x, o.y ).mul( Director.scalingStrategy.invTileMapZoomFactor );
			pos.y = Director.worldSizeScaledPx.y - pos.y;
			pos.set( Convert.px2mt( pos ));
//			System.out.println("Light @ " + pos);

			PointLight l = new PointLight( rayHandler, maxRays, c, 30f, pos.x, pos.y );
			l.setSoft( false );
			l.setMaskBits( CollisionFilters.CategoryPlayer | CollisionFilters.CategoryTrackWalls );
		}
	}

	public void renderLights()
	{
		// update player light (subframe interpolation ready)
		float ang = 90 + player.car.state().orientation;

		// the body's compound shape should be created with some clever thinking in it :)
		float offx = (player.car.getCarModel().length/2f) + .25f;
		float offy = 0f;

		float cos = MathUtils.cosDeg(ang);
		float sin = MathUtils.sinDeg(ang);
		float dX = offx * cos - offy * sin;
		float dY = offx * sin + offy * cos;

		float px = Convert.px2mt(player.car.state().position.x) + dX;
		float py = Convert.px2mt(player.car.state().position.y) + dY;

		playerHeadlights.setDirection( ang );
		playerHeadlights.setPosition( px, py );

		rayHandler.setCombinedMatrix
		(
			Director.getMatViewProjMt(),
			Convert.px2mt(camOrtho.position.x * Director.scalingStrategy.invTileMapZoomFactor),
			Convert.px2mt(camOrtho.position.y * Director.scalingStrategy.invTileMapZoomFactor),
			Convert.px2mt(camOrtho.viewportWidth),
			Convert.px2mt(camOrtho.viewportHeight)
		);

		rayHandler.update();
		rayHandler.render();

		if( Config.isDesktop && (URacer.getFrameCount()&0x1f)==0x1f)
		{
			System.out.println("lights rendered="+rayHandler.lightRenderedLastFrame);
		}
	}

	/**
	 * operations
	 */

	public Player getPlayer()
	{
		return player;
	}

	public boolean isNightMode()
	{
		return nightMode;
	}

	public void beginRecording( Replay outputBuffer, long lapStartTimeNs )
	{
		recorder.beginRecording( player.car, outputBuffer, lapStartTimeNs );
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

	public void reset()
	{
		player.reset();
		recorder.reset();
	}
}
