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
import com.bitfire.uracer.Director;
import com.bitfire.uracer.utils.MapUtils;

/**
 * First write. Basic idea in place, will need refactoring for sure.
 *
 * @author manuel
 *
 */
public class Level
{
	public TiledMap map;
	public TileMapRenderer renderer;
	public Track track;

	public Level( String levelName, ScalingStrategy strategy )
	{
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

	public void init()
	{
		// create track
		track = new Track( map );

		createObjects();
	}

	public void syncWithCam( OrthographicCamera camera_ )
	{
		// scale position
		camOrtho.position.set( camera_.position );
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

		for( int i = 0; i < meshes.size(); i++ )
		{
			OrthographicAlignedMesh t = meshes.get( i );
			t.render( gl, camOrtho, camPersp );
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

	public void createObjects()
	{
		// first object group is the static meshes container
		if( !MapUtils.hasObjectGroup( MapUtils.LayerStaticMeshes ) ) return;

		TiledObjectGroup group = MapUtils.getObjectGroup( MapUtils.LayerStaticMeshes );
		for( int i = 0; i < group.objects.size(); i++ )
		{
			TiledObject o = group.objects.get( i );

			float scale = 1f;
			if( o.properties.get( MapUtils.MeshScale ) != null )
				scale = Float.parseFloat( o.properties.get( MapUtils.MeshScale ) );

			// System.out.println("Creating " + o.type + ", [" + o.x + "," + o.y
			// + "] x" + scale);
			meshes.add( MeshFactory.create( o.type, o.x, o.y, scale ) );
		}

		// apply horizontal fov scaling factor distortion and blender factors

		// Blender => cube 14.2x14.2 meters = one tile (256px) w/ far plane @48
		// (256px are 14.2mt w/ 18px/mt)
		// I'm lazy and want Blender to work with 10x10mt instead, so a 1.42f
		// factor for this scaling: also, since the far plane is suboptimal at
		// just 48, i want 5 times more space on the z-axis, so here's another
		// scaling factor creeping up.
		float blenderToUracer = 5f * 1.42f;

		for( int i = 0; i < meshes.size(); i++ )
		{
			OrthographicAlignedMesh t = meshes.get( i );

			// note about the "Convert.to256" factor
			//
			// object scales where defined for a tilesize of 256px at the target
			// screen resolution
			// let's scale back in case the tilesize is different
			t.rescale( Director.scalingStrategy.meshScaleFactor * blenderToUracer * Director.scalingStrategy.to256 );
		}
	}

	private static final String LevelsStore = "data/levels/";
	private TileAtlas atlas;
	private PerspectiveCamera camPersp;
	private OrthographicCamera camOrtho;
	private ArrayList<OrthographicAlignedMesh> meshes = new ArrayList<OrthographicAlignedMesh>();
	private float camPerspElevation;
}
