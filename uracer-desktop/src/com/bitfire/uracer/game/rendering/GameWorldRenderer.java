package com.bitfire.uracer.game.rendering;

import java.util.List;

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
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.configuration.Storage;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.logic.helpers.CameraController;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.game.world.models.OrthographicAlignedStillModel;
import com.bitfire.uracer.game.world.models.TrackTrees;
import com.bitfire.uracer.game.world.models.TrackWalls;
import com.bitfire.uracer.game.world.models.TreeStillModel;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.utils.ShaderLoader;

public final class GameWorldRenderer {
	// @formatter:off
	private static final String treeVertexShader =
		"uniform mat4 u_projTrans;					\n" +
		"attribute vec4 a_position;					\n" +
		"attribute vec2 a_texCoord0;				\n" +
		"varying vec2 v_TexCoord;					\n" +
		"void main()								\n" +
		"{											\n" +
		"	gl_Position = u_projTrans * a_position;	\n" +
		"	v_TexCoord = a_texCoord0;				\n" +
		"}											\n";

	private static final String treeFragmentShader =
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

	// the game world
	private GameWorld world = null;

	// camera view
	private PerspectiveCamera camPersp = null;
	protected OrthographicCamera camTilemap = null;
	protected OrthographicCamera camOrtho = null;
	protected Vector2 halfViewport = new Vector2();
	protected Rectangle camOrthoRect = new Rectangle();
	private Matrix4 camOrthoMvpMt = new Matrix4();
	private Matrix4 camPerspInvProjView = new Matrix4();
	private Matrix4 camPerspPrevProjView = new Matrix4();
	private CameraController camController;
	private static final float CamPerspPlaneNear = 1;
	private static final float CamPerspPlaneFar = 240;
	private static final float CamPerspElevation = 100f;

	// rendering
	private GL20 gl = null;
	private ShaderProgram treeShader = null;
	private TileAtlas tileAtlas = null;
	private boolean renderPlayerHeadlights = true;

	public UTileMapRenderer tileMapRenderer = null;
	private ScalingStrategy scalingStrategy = null;
	private float scaledPpm = 1f;

	// render stats
	private ImmediateModeRenderer20 dbg = new ImmediateModeRenderer20( false, true, 0 );
	public static int renderedTrees = 0;
	public static int renderedWalls = 0;
	public static int culledMeshes = 0;

	// world refs
	private RayHandler rayHandler = null;
	private List<OrthographicAlignedStillModel> staticMeshes = null;
	private TrackTrees trackTrees = null;
	private TrackWalls trackWalls = null;
	private ConeLight playerLights = null;

	public GameWorldRenderer( ScalingStrategy strategy, GameWorld world, int width, int height ) {
		scalingStrategy = strategy;
		this.world = world;
		gl = Gdx.gl20;
		scaledPpm = Convert.scaledPixels( Config.Physics.PixelsPerMeter );
		rayHandler = world.getRayHandler();
		trackTrees = world.getTrackTrees();
		trackWalls = world.getTrackWalls();
		playerLights = world.getPlayerHeadLights();
		staticMeshes = world.getStaticMeshes();

		createCams( width, height );

		FileHandle baseDir = Gdx.files.internal( Storage.Levels );
		tileAtlas = new TileAtlas( world.map, baseDir );
		tileMapRenderer = new UTileMapRenderer( world.map, tileAtlas, 1, 1, world.map.tileWidth, world.map.tileHeight );

		treeShader = ShaderLoader.fromString( treeVertexShader, treeFragmentShader, "tree-fragment", "tree-vertex" );

		if( !treeShader.isCompiled() ) {
			throw new IllegalStateException( treeShader.getLog() );
		}
	}

	public void dispose() {
		tileMapRenderer.dispose();
		tileAtlas.dispose();
	}

	private void createCams( int width, int height ) {
		camOrtho = new OrthographicCamera( width, height );
		halfViewport.set( camOrtho.viewportWidth / 2, camOrtho.viewportHeight / 2 );

		// creates and setup orthographic camera
		camTilemap = new OrthographicCamera( Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );
		// camTilemap.near = 0;
		// camTilemap.far = 100;
		camTilemap.zoom = 1;

		// creates and setup perspective camera
		// strategically choosen near/far planes, Blender models' 14.2 meters <=> one 256px tile
		// with far plane @48
		camPersp = new PerspectiveCamera( scalingStrategy.verticalFov, width, height );
		camPersp.near = CamPerspPlaneNear;
		camPersp.far = CamPerspPlaneFar;
		camPersp.lookAt( 0, 0, -1 );
		camPersp.position.set( camTilemap.position.x, camTilemap.position.y, CamPerspElevation );
		camPersp.update();

		camController = new CameraController( Config.Graphics.CameraInterpolationMode, halfViewport, world.worldSizeScaledPx,
				world.worldSizeTiles );
	}

	public OrthographicCamera getOrthographicCamera() {
		return camOrtho;
	}

	public Matrix4 getOrthographicMvpMt() {
		return camOrthoMvpMt;
	}

	public void setRenderPlayerHeadlights( boolean value ) {
		renderPlayerHeadlights = value;
		if( playerLights != null ) {
			playerLights.setActive( value );
		}
	}

	public Matrix4 getInvProjView() {
		return camPerspInvProjView;
	}

	public Matrix4 getPrevProjView() {
		return camPerspPrevProjView;
	}

	public void resetCounters() {
		culledMeshes = 0;
		renderedTrees = 0;
		renderedWalls = 0;
	}

	public void updatePlayerHeadlights( Car car ) {
		if( renderPlayerHeadlights && car != null ) {
			Vector2 carPosition = car.state().position;
			float carOrientation = car.state().orientation;
			float carLength = car.getCarModel().length;

			// update player light (subframe interpolation ready)
			float ang = 90 + carOrientation;

			// the body's compound shape should be created with some clever thinking in it :)
			float offx = (carLength / 2f) + .25f;
			float offy = 0f;

			float cos = MathUtils.cosDeg( ang );
			float sin = MathUtils.sinDeg( ang );
			float dX = offx * cos - offy * sin;
			float dY = offx * sin + offy * cos;

			float px = Convert.px2mt( carPosition.x ) + dX;
			float py = Convert.px2mt( carPosition.y ) + dY;

			playerLights.setDirection( ang );
			playerLights.setPosition( px, py );
		}

		// if( Config.isDesktop && (URacer.getFrameCount() & 0x1f) == 0x1f ) {
		// System.out.println( "lights rendered=" + rayHandler.lightRenderedLastFrame );
		// }
	}

	private void updateRayHandler() {
		if( rayHandler != null ) {

			// @formatter:off
			rayHandler.setCombinedMatrix( camOrthoMvpMt,
					Convert.px2mt( camOrtho.position.x ),
					Convert.px2mt( camOrtho.position.y ),
					Convert.px2mt( camOrtho.viewportWidth ),
					Convert.px2mt( camOrtho.viewportHeight ) );
			// @formatter:on

			rayHandler.update();
			// Gdx.app.log( "GameWorldRenderer", "lights rendered=" + rayHandler.lightRenderedLastFrame );

			rayHandler.updateLightMap();
		}
	}

	private Vector2 cameraPos = new Vector2();

	public void setInitialCameraPositionOrient( Car car ) {
		cameraPos.set( Convert.mt2px( car.getWorldPosMt() ) );
		camController.setInitialPositionOrient( cameraPos, car.getWorldOrientRads() * MathUtils.radiansToDegrees );
	}

	public void setCameraPosition( GhostCar ghost ) {
		cameraPos.set( camController.transform( ghost.state().position, ghost.state().orientation, 0 ) );
	}

	public void setCameraPosition( PlayerCar player ) {
		cameraPos.set( camController.transform( player.state().position, player.state().orientation,
				player.carState.currSpeedFactor ) );
	}

	public void setCameraPosition( Vector2 position, float orient ) {
		cameraPos.set( camController.transform( position, orient, 0 ) );
	}

	public void onBeforeRender() {
		// update orthographic camera

		// remove subpixel accuracy (jagged behavior)
		camOrtho.position.x = MathUtils.round( cameraPos.x );
		camOrtho.position.y = MathUtils.round( cameraPos.y );
		camOrtho.position.z = 0;
		camOrtho.update();

		// update the unscaled orthographic camera rectangle, for visibility queries
		camOrthoRect.set( camOrtho.position.x - halfViewport.x, camOrtho.position.y - halfViewport.y, camOrtho.viewportWidth,
				camOrtho.viewportHeight );

		// update the model-view-projection matrix, in meters, from the unscaled orthographic camera
		camOrthoMvpMt.set( camOrtho.combined );
		camOrthoMvpMt.val[Matrix4.M00] *= scaledPpm;
		camOrthoMvpMt.val[Matrix4.M01] *= scaledPpm;
		camOrthoMvpMt.val[Matrix4.M10] *= scaledPpm;
		camOrthoMvpMt.val[Matrix4.M11] *= scaledPpm;

		// update the tilemap renderer orthographic camera
		camTilemap.position.set( camOrtho.position ).mul( scalingStrategy.tileMapZoomFactor );
		camTilemap.zoom = scalingStrategy.tileMapZoomFactor;
		camTilemap.update();

		// update previous proj view
		camPerspPrevProjView.set( camPersp.combined );

		// sync perspective camera to the orthographic camera
		camPersp.position.set( camTilemap.position.x, camTilemap.position.y, CamPerspElevation );
		camPersp.update();

		// update inv proj view
		camPerspInvProjView.set( camPersp.invProjectionView );

		updateRayHandler();
	}

	public void renderLigthMap( FrameBuffer dest ) {
		rayHandler.renderLightMap( dest );
	}

	public void renderTilemap() {
		gl.glDisable( GL20.GL_BLEND );
		gl.glActiveTexture( GL10.GL_TEXTURE0 );
		tileMapRenderer.render( camTilemap );
	}

	private void renderWalls( TrackWalls walls, boolean depthOnly ) {
		if( !depthOnly ) {
			gl.glEnable( GL20.GL_BLEND );
			gl.glBlendFunc( GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA );
		}

		gl.glDisable( GL20.GL_CULL_FACE );
		renderedWalls = renderOrthographicAlignedModels( walls.models, depthOnly );
	}

	private void renderTrees( TrackTrees trees, boolean depthOnly ) {
		trees.transform( camPersp, camOrtho, halfViewport );

		ShaderProgram shader = treeShader;

		gl.glEnable( GL20.GL_CULL_FACE );
		gl.glDisable( GL20.GL_BLEND );

		// if( depthOnly ) {
		// shader = Art.depthMapGen;
		// } else {
		Art.meshTreeTrunk.bind();
		// }

		shader.begin();

		// all the trunks
		for( int i = 0; i < trees.models.size(); i++ ) {
			TreeStillModel m = trees.models.get( i );
			shader.setUniformMatrix( "u_projTrans", m.transformed );
			m.trunk.render( shader, m.smTrunk.primitiveType );
		}

		// all the transparent foliage
		// gl.glDisable( GL20.GL_CULL_FACE );

		if( !depthOnly ) {
			gl.glEnable( GL20.GL_BLEND );
			gl.glBlendFunc( GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA );
		}

		boolean needRebind = false;
		for( int i = 0; i < trees.models.size(); i++ ) {
			TreeStillModel m = trees.models.get( i );

			if( Config.Debug.FrustumCulling && !camPersp.frustum.boundsInFrustum( m.boundingBox ) ) {
				needRebind = true;
				culledMeshes++;
				continue;
			}

			shader.setUniformMatrix( "u_projTrans", m.transformed );

			// if( !depthOnly )
			{
				if( i == 0 || needRebind ) {
					m.material.bind( shader );
				} else if( !trees.models.get( i - 1 ).material.equals( m.material ) ) {
					m.material.bind( shader );
				}
			}

			m.leaves.render( shader, m.smLeaves.primitiveType );

			renderedTrees++;
		}

		shader.end();

		if( !depthOnly && Config.Debug.Render3DBoundingBoxes ) {
			// debug
			for( int i = 0; i < trees.models.size(); i++ ) {
				TreeStillModel m = trees.models.get( i );
				renderBoundingBox( m.boundingBox );
			}
		}
	}

	private Vector3 tmpvec = new Vector3();
	private Matrix4 mtx = new Matrix4();
	private Matrix4 mtx2 = new Matrix4();
	private Vector2 pospx = new Vector2();

	private int renderOrthographicAlignedModels( List<OrthographicAlignedStillModel> models, boolean depthOnly ) {
		int renderedCount = 0;
		OrthographicAlignedStillModel m;
		StillSubMesh submesh;

		float meshZ = -(camPersp.far - camPersp.position.z);

		ShaderProgram shader = OrthographicAlignedStillModel.shader;

		// if( depthOnly ) {
		// shader = Art.depthMapGen;
		// }

		shader.begin();

		boolean needRebind = false;
		for( int i = 0; i < models.size(); i++ ) {
			m = models.get( i );
			submesh = m.model.subMeshes[0];

			// compute position
			pospx.set( m.positionPx );
			pospx.set( world.positionFor( pospx ) );
			tmpvec.x = (m.positionOffsetPx.x - camOrtho.position.x) + halfViewport.x + pospx.x;
			tmpvec.y = (m.positionOffsetPx.y + camOrtho.position.y) + halfViewport.y - pospx.y;
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

			// ensure the bounding box is transformed
			m.boundingBox.inf().set( m.localBoundingBox );
			m.boundingBox.mul( mtx );

			// perform culling
			if( Config.Debug.FrustumCulling && !camPersp.frustum.boundsInFrustum( m.boundingBox ) ) {
				needRebind = true;
				culledMeshes++;
				continue;
			}

			shader.setUniformMatrix( "u_projTrans", mtx2 );

			// if( !depthOnly )
			{
				// avoid rebinding same textures
				if( i == 0 || needRebind ) {
					m.material.bind( shader );
				} else if( !models.get( i - 1 ).material.equals( m.material ) ) {
					m.material.bind( shader );
				}
			}

			submesh.mesh.render( shader, submesh.primitiveType );
			renderedCount++;
		}

		shader.end();

		if( !depthOnly && Config.Debug.Render3DBoundingBoxes ) {
			// debug (tested on a single mesh only!)
			for( int i = 0; i < models.size(); i++ ) {
				m = models.get( i );
				renderBoundingBox( m.boundingBox );
			}
		}

		return renderedCount;
	}

	public void renderAllMeshes( boolean depthOnly ) {
		resetCounters();

		gl.glEnable( GL20.GL_DEPTH_TEST );
		gl.glDepthFunc( GL20.GL_LEQUAL );

		if( trackWalls.count() > 0 ) {
			renderWalls( trackWalls, depthOnly );
		}

		if( trackTrees.count() > 0 ) {
			renderTrees( trackTrees, depthOnly );
		}

		// render "static-meshes" layer
		gl.glEnable( GL20.GL_CULL_FACE );
		gl.glFrontFace( GL20.GL_CCW );
		gl.glCullFace( GL20.GL_BACK );

		renderOrthographicAlignedModels( staticMeshes, depthOnly );

		gl.glDisable( GL20.GL_CULL_FACE );
		gl.glDisable( GL20.GL_DEPTH_TEST );
	}

	/**
	 * This is intentionally SLOW. Read it again!
	 * 
	 * @param boundingBox
	 */
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
