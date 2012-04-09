package com.bitfire.uracer.game.rendering;

import java.util.ArrayList;

import box2dLight.ConeLight;
import box2dLight.RayHandler;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.tiled.TileAtlas;
import com.badlogic.gdx.graphics.g3d.model.still.StillSubMesh;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.game.GameData;
import com.bitfire.uracer.game.GameData.States;
import com.bitfire.uracer.game.entities.Car;
import com.bitfire.uracer.game.GameWorld;
import com.bitfire.uracer.tiled.OrthographicAlignedStillModel;
import com.bitfire.uracer.tiled.TrackTrees;
import com.bitfire.uracer.tiled.TrackWalls;
import com.bitfire.uracer.tiled.TreeStillModel;
import com.bitfire.uracer.tiled.UTileMapRenderer;
import com.bitfire.uracer.utils.Convert;

public class GameWorldRenderer {
	// @formatter:off
	private static final String vertexShader =
		"uniform mat4 u_mvpMatrix;					\n" +
		"attribute vec4 a_position;					\n" +
		"attribute vec2 a_texCoord0;				\n" +
		"varying vec2 v_TexCoord;					\n" +
		"void main()								\n" +
		"{											\n" +
		"	gl_Position = u_mvpMatrix * a_position;	\n" +
		"	v_TexCoord = a_texCoord0;				\n" +
		"}											\n";

	private static final String fragmentShader =
		"#ifdef GL_ES											\n" +
		"precision mediump float;								\n" +
		"#endif													\n" +
		"uniform sampler2D u_texture;							\n" +
		"varying vec2 v_TexCoord;								\n" +
		"void main()											\n" +
		"{														\n" +
		"	vec4 texel = texture2D( u_texture, v_TexCoord );	\n" +
		"	if(texel.a < 0.5) discard;							\n" +
		"	gl_FragColor = texel;								\n" +
		"}														\n";
	// @formatter:on

	private PerspectiveCamera camPersp = null;
	private OrthographicCamera camOrtho = null;
	private ShaderProgram treeShader = null;
	private float camPerspElevation = 0f;
	private TileAtlas tileAtlas = null;

	public UTileMapRenderer tileMapRenderer = null;

	// render stats
	private ImmediateModeRenderer20 dbg = new ImmediateModeRenderer20( false, true, 0 );
	public static int renderedTrees = 0;
	public static int renderedWalls = 0;
	public static int culledMeshes = 0;

	// world refs
	private RayHandler rayHandler = null;
	private ArrayList<OrthographicAlignedStillModel> staticMeshes = null;
	private TrackTrees trackTrees = null;
	private TrackWalls trackWalls = null;
	private ConeLight playerLights = null;

	public GameWorldRenderer( GameWorld world, int width, int height ) {
		rayHandler = world.getRayHandler();
		trackTrees = world.getTrackTrees();
		trackWalls = world.getTrackWalls();
		playerLights = world.getPlayerHeadLights();
		staticMeshes = world.getStaticMeshes();

		createCams( width, height );

		FileHandle baseDir = Gdx.files.internal( Config.LevelsStore );
		tileAtlas = new TileAtlas( world.map, baseDir );
		tileMapRenderer = new UTileMapRenderer( world.map, tileAtlas, 1, 1, world.map.tileWidth, world.map.tileHeight );

		ShaderProgram.pedantic = false;
		treeShader = new ShaderProgram( vertexShader, fragmentShader );

		if( treeShader.isCompiled() == false )
			throw new IllegalStateException( treeShader.getLog() );
	}

	public void dispose() {
		tileMapRenderer.dispose();
		tileAtlas.dispose();
	}

	public void resetCounters() {
		culledMeshes = renderedTrees = renderedWalls = 0;
	}

	public void generateLightMap() {
		Car car = States.playerState.car;

		// update player light (subframe interpolation ready)
		float ang = 90 + car.state().orientation;

		// the body's compound shape should be created with some clever thinking in it :)
		float offx = (car.getCarModel().length / 2f) + .25f;
		float offy = 0f;

		float cos = MathUtils.cosDeg( ang );
		float sin = MathUtils.sinDeg( ang );
		float dX = offx * cos - offy * sin;
		float dY = offx * sin + offy * cos;

		float px = Convert.px2mt( car.state().position.x ) + dX;
		float py = Convert.px2mt( car.state().position.y ) + dY;

		playerLights.setDirection( ang );
		playerLights.setPosition( px, py );

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

	private void renderWalls( GL20 gl, TrackWalls walls ) {
		if( walls.walls.size() > 0 ) {
			gl.glDisable( GL20.GL_CULL_FACE );
			gl.glEnable( GL20.GL_BLEND );
			gl.glBlendFunc( GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA );
			renderedWalls = renderOrthographicAlignedModels( gl, walls.walls );
		}
	}

	private void renderTrees( GL20 gl, TrackTrees trees ) {
		if( trees.trees.size() > 0 ) {
			trees.transform( camPersp, camOrtho );

			gl.glDisable( GL20.GL_BLEND );
			gl.glEnable( GL20.GL_CULL_FACE );

			Art.meshTreeTrunk.bind();

			treeShader.begin();

			// trunk
			for( int i = 0; i < trees.trees.size(); i++ ) {
				TreeStillModel m = trees.trees.get( i );
				treeShader.setUniformMatrix( "u_mvpMatrix", m.transformed );
				m.trunk.render( treeShader, m.smTrunk.primitiveType );
			}

			// transparent foliage
			gl.glDisable( GL20.GL_CULL_FACE );
			gl.glEnable( GL20.GL_BLEND );
			gl.glBlendFunc( GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA );

			boolean needRebind = false;
			for( int i = 0; i < trees.trees.size(); i++ ) {
				TreeStillModel m = trees.trees.get( i );

				if( Config.Debug.FrustumCulling && !camPersp.frustum.boundsInFrustum( m.boundingBox ) ) {
					needRebind = true;
					culledMeshes++;
					continue;
				}

				if( i == 0 || needRebind ) {
					m.material.bind( treeShader );
				} else if( !trees.trees.get( i - 1 ).material.equals( m.material ) ) {
					m.material.bind( treeShader );
				}

				treeShader.setUniformMatrix( "u_mvpMatrix", m.transformed );
				m.leaves.render( treeShader, m.smLeaves.primitiveType );

				renderedTrees++;
			}

			treeShader.end();

			if( Config.Graphics.Render3DBoundingBoxes ) {
				// debug
				for( int i = 0; i < trees.trees.size(); i++ ) {
					TreeStillModel m = trees.trees.get( i );
					renderBoundingBox( m.boundingBox );
				}
			}
		}
	}

	private Vector3 tmpvec = new Vector3();
	private Matrix4 mtx = new Matrix4();
	private Matrix4 mtx2 = new Matrix4();

	private int renderOrthographicAlignedModels( GL20 gl, ArrayList<OrthographicAlignedStillModel> models ) {
		int renderedCount = 0;
		OrthographicAlignedStillModel m;
		StillSubMesh submesh;

		float meshZ = -(camPersp.far - camPersp.position.z);

		ShaderProgram shader = OrthographicAlignedStillModel.shader;
		shader.begin();

		boolean needRebind = false;
		for( int i = 0; i < models.size(); i++ ) {
			m = models.get( i );
			submesh = m.model.subMeshes[0];

			// compute position
			tmpvec.x = Convert.scaledPixels( m.positionOffsetPx.x - camOrtho.position.x ) + Director.halfViewport.x + m.positionPx.x;
			tmpvec.y = Convert.scaledPixels( m.positionOffsetPx.y + camOrtho.position.y ) + Director.halfViewport.y - m.positionPx.y;
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

			// transform the bounding box
			m.boundingBox.inf().set( m.localBoundingBox );
			m.boundingBox.mul( mtx );

			if( Config.Debug.FrustumCulling && !camPersp.frustum.boundsInFrustum( m.boundingBox ) ) {
				needRebind = true;
				culledMeshes++;
				continue;
			}

			shader.setUniformMatrix( "u_mvpMatrix", mtx2 );

			// avoid rebinding same textures
			if( i == 0 || needRebind ) {
				m.material.bind( shader );
			} else if( !models.get( i - 1 ).material.equals( m.material ) ) {
				m.material.bind( shader );
			}

			submesh.mesh.render( OrthographicAlignedStillModel.shader, submesh.primitiveType );
			renderedCount++;
		}

		shader.end();

		if( Config.Graphics.Render3DBoundingBoxes ) {
			// debug (tested on a single mesh only!)
			for( int i = 0; i < models.size(); i++ ) {
				m = models.get( i );
				renderBoundingBox( m.boundingBox );
			}
		}

		return renderedCount;
	}

	public void renderTilemap( GL20 gl ) {
		gl.glDisable( GL20.GL_BLEND );
		tileMapRenderer.render( camOrtho );
	}

	public void renderAllMeshes( GL20 gl ) {
		resetCounters();

		gl.glDepthMask( true );
		gl.glEnable( GL20.GL_DEPTH_TEST );
		gl.glCullFace( GL20.GL_BACK );
		gl.glFrontFace( GL20.GL_CCW );
		gl.glDepthFunc( GL20.GL_LESS );
		gl.glBlendEquation( GL20.GL_FUNC_ADD );

		renderWalls( gl, trackWalls );
		renderTrees( gl, trackTrees );

		// render "static-meshes" layer
		gl.glEnable( GL20.GL_CULL_FACE );
		renderOrthographicAlignedModels( gl, staticMeshes );

		gl.glDisable( GL20.GL_DEPTH_TEST );
		gl.glDisable( GL20.GL_CULL_FACE );
		gl.glDepthMask( false );
	}

	/** This is intentionally SLOW. Read it again!
	 *
	 * @param boundingBox */
	private void renderBoundingBox( BoundingBox boundingBox ) {
		float alpha = .15f;
		float r = 0f;
		float g = 0f;
		float b = 1f;
		float offset = 0.5f;	// offset for the base, due to pixel-perfect model placement

		Vector3[] corners = boundingBox.getCorners();

		Gdx.gl.glDisable( GL20.GL_CULL_FACE );
		Gdx.gl.glEnable( GL20.GL_BLEND );
		Gdx.gl.glBlendFunc( GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA );

		dbg.begin( camPersp.combined, GL10.GL_TRIANGLES );
		{
			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[0].x, corners[0].y, corners[0].z + offset );
			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[1].x, corners[1].y, corners[1].z + offset );
			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[4].x, corners[4].y, corners[4].z );

			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[1].x, corners[1].y, corners[1].z + offset );
			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[4].x, corners[4].y, corners[4].z );
			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[5].x, corners[5].y, corners[5].z );

			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[1].x, corners[1].y, corners[1].z + offset );
			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[2].x, corners[2].y, corners[2].z + offset );
			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[5].x, corners[5].y, corners[5].z );

			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[2].x, corners[2].y, corners[2].z + offset );
			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[5].x, corners[5].y, corners[5].z );
			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[6].x, corners[6].y, corners[6].z );

			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[2].x, corners[2].y, corners[2].z + offset );
			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[6].x, corners[6].y, corners[6].z );
			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[3].x, corners[3].y, corners[3].z + offset );

			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[3].x, corners[3].y, corners[3].z );
			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[6].x, corners[6].y, corners[6].z );
			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[7].x, corners[7].y, corners[7].z );

			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[3].x, corners[3].y, corners[3].z + offset );
			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[0].x, corners[0].y, corners[0].z + offset );
			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[7].x, corners[7].y, corners[7].z );

			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[7].x, corners[7].y, corners[7].z );
			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[0].x, corners[0].y, corners[0].z + offset );
			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[4].x, corners[4].y, corners[4].z );

			// top cap
			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[4].x, corners[4].y, corners[4].z );
			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[5].x, corners[5].y, corners[5].z );
			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[7].x, corners[7].y, corners[7].z );

			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[5].x, corners[5].y, corners[5].z );
			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[7].x, corners[7].y, corners[7].z );
			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[6].x, corners[6].y, corners[6].z );

			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[0].x, corners[0].y, corners[0].z + offset );
			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[3].x, corners[3].y, corners[3].z + offset );
			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[1].x, corners[1].y, corners[1].z + offset );

			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[3].x, corners[3].y, corners[3].z + offset );
			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[1].x, corners[1].y, corners[1].z + offset );
			dbg.color( r, g, b, alpha );
			dbg.vertex( corners[2].x, corners[2].y, corners[2].z + offset );
		}
		dbg.end();

		Gdx.gl.glDisable( GL20.GL_BLEND );
	}
}
