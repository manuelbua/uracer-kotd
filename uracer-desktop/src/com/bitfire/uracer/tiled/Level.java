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
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Director;

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

		createCams();

		// should be read from the level file descriptor
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
		float perspPlaneFar = 500;
		camPerspElevation = 100;

		camPersp = new PerspectiveCamera( Director.scalingStrategy.verticalFov, w, h );
		camPersp.near = perspPlaneNear;
		camPersp.far = perspPlaneFar;
		camPersp.lookAt( 0, 0, -1 );
		camPersp.position.set( 0, 0, camPerspElevation );
	}


	private void createObjects()
	{
		// object scales where defined for a tilesize of 256px at the target
		// screen resolution
		// let's scale back in case the tilesize is different
		float to256 = Director.scalingStrategy.tileSizeAtRef / 256f;

		float scalePalm = 6f * to256;
		float scaleHouse = 16.2f * to256;
		float scaleTribune = 2.1f * to256;
		float scaleTower = 5f * to256;

		int tilesize = map.tileWidth;

		OrthographicAlignedMesh mesh;

		// simple
		mesh = OrthographicAlignedMesh.create( map, "data/3d/simple.g3dt", "data/3d/tower.png", new Vector2( 4, 3 ) );
		mesh.setScale( 10.4f );
//		mesh.setPositionOffsetPixels( -tilesize / 2, -tilesize / 2 );
		meshes.add( mesh );

		// palm #1
		mesh = OrthographicAlignedMesh.create( map, "data/3d/palm.g3dt", "data/3d/palm.png", new Vector2( 0, 0 ) );
		mesh.setScale( scalePalm );
		mesh.setPositionOffsetPixels( -tilesize / 4, -tilesize / 4 );
		meshes.add( mesh );

		// palm #2
		mesh = OrthographicAlignedMesh.create( map, "data/3d/palm.g3dt", "data/3d/palm.png", new Vector2( 3, 1 ) );
		mesh.setScale( scalePalm );
		mesh.setPositionOffsetPixels( -tilesize / 4, -tilesize / 4 );
		meshes.add( mesh );

		// palm #3
		mesh = OrthographicAlignedMesh.create( map, "data/3d/palm.g3dt", "data/3d/palm.png", new Vector2( 2, 4 ) );
		mesh.setScale( scalePalm * 1.4f );
		mesh.setPositionOffsetPixels( -tilesize / 4, -tilesize / 4 );
		meshes.add( mesh );

		// palm #4
		mesh = OrthographicAlignedMesh.create( map, "data/3d/palm.g3dt", "data/3d/palm.png", new Vector2( 0, 4 ) );
		mesh.setScale( scalePalm );
		mesh.setPositionOffsetPixels( -tilesize / 4, -tilesize / 4 );
		meshes.add( mesh );

		// palm #5
		mesh = OrthographicAlignedMesh.create( map, "data/3d/palm.g3dt", "data/3d/palm.png", new Vector2( 0, 5 ) );
		mesh.setScale( scalePalm );
		mesh.setPositionOffsetPixels( -tilesize / 4, -tilesize / 4 );
		meshes.add( mesh );

		// palm #6
		mesh = OrthographicAlignedMesh.create( map, "data/3d/palm.g3dt", "data/3d/palm.png", new Vector2( 1, 5 ) );
		mesh.setScale( scalePalm * 0.45f );
		mesh.setPositionOffsetPixels( -tilesize / 3, -tilesize / 3 );
		meshes.add( mesh );

		// palm #7
		mesh = OrthographicAlignedMesh.create( map, "data/3d/palm.g3dt", "data/3d/palm.png", new Vector2( 1, 5 ) );
		mesh.setScale( scalePalm * 0.55f );
		mesh.setPositionOffsetPixels( -tilesize / 2, -tilesize / 2 );
		meshes.add( mesh );

		// house
		mesh = OrthographicAlignedMesh.create( map, "data/3d/house.g3dt", "data/3d/house.png", new Vector2( 1, 2 ) );
		mesh.setScale( scaleHouse );
		mesh.setPositionOffsetPixels( 0, tilesize / 4 );
		meshes.add( mesh );

		// tribune
		mesh = OrthographicAlignedMesh.create( map, "data/3d/tribune.g3dt", "data/3d/tribune.png", new Vector2( 6, 1 ) );
		mesh.setScale( scaleTribune );
		meshes.add( mesh );

		// towers
		for( int i = 0; i < 5; i++ )
		{
			OrthographicAlignedMesh t = OrthographicAlignedMesh.create( map, "data/3d/tower.g3dt", "data/3d/tower.png" );
			switch( i )
			{
			case 0:
				t.setTilePosition( 4, 1 );
				t.setRotation( 90, 0, 0, 1 );
				break;

			case 1:
				t.setTilePosition( 4, 3 );
				t.setRotation( 90, 0, 0, -1 );
				break;

			case 2:
				t.setTilePosition( 4, 4 );
				break;

			case 3:
				t.setTilePosition( 4, 5 );
				break;

			case 4:
				t.setTilePosition( 6, 3 );
				break;
			}

			t.setScale( scaleTower * 0.8f );
			meshes.add( t );
		}

		// apply horizontal fov scaling factor distortion
		float scale = Director.scalingStrategy.meshScaleFactor;
		for( int i = 0; i < meshes.size(); i++ )
		{
			OrthographicAlignedMesh t = meshes.get( i );
			t.rescale( scale );
		}
	}

	private static final String LevelsStore = "data/levels/";
	private TileAtlas atlas;
	private PerspectiveCamera camPersp;
	private OrthographicCamera camOrtho;
	private ArrayList<OrthographicAlignedMesh> meshes = new ArrayList<OrthographicAlignedMesh>();
	private float camPerspElevation;
}
