package com.bitfire.uracer.game.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.postprocessing.PostProcessor;
import com.bitfire.uracer.utils.Convert;

/** Manages the high-level rendering of the whole world and triggers all the GameRendererEvent events
 * associated with the rendering timeline, realized with the event's renderqueue mechanism.
 *
 * @author bmanuel */
public final class GameRenderer {
	private final GL20 gl;
	private final GameWorld world;
	private final GameBatchRenderer batchRenderer;
	private final PostProcessor postProcessor;
	private final GameWorldRenderer worldRenderer;

	/** Manages to convert world positions expressed in meters or pixels to the corresponding position to screen pixels.
	 * To use this class, the GameWorldRenderer MUST be already constructed and initialized. */
	public static final class ScreenUtils {
		public static boolean ready = false;
		private static Vector2 screenPosFor = new Vector2();
		private static GameWorldRenderer worldRenderer;

		public static void init( GameWorldRenderer worldRenderer ) {
			ScreenUtils.worldRenderer = worldRenderer;
			ScreenUtils.ready = true;
		}

		public static Vector2 worldMtToScreen( Vector2 worldPositionMt ) {
			screenPosFor.x = Convert.mt2px( worldPositionMt.x ) - worldRenderer.camOrtho.position.x + worldRenderer.halfViewport.x;
			screenPosFor.y = worldRenderer.camOrtho.position.y - Convert.mt2px( worldPositionMt.y ) + worldRenderer.halfViewport.y;
			return screenPosFor;
		}

		public static Vector2 worldPxToScreen( Vector2 worldPositionPx ) {
			screenPosFor.x = worldPositionPx.x - worldRenderer.camOrtho.position.x + worldRenderer.halfViewport.x;
			screenPosFor.y = worldRenderer.camOrtho.position.y - worldPositionPx.y + worldRenderer.halfViewport.y;
			return screenPosFor;
		}

		public static boolean isVisible( Rectangle rect ) {
			return worldRenderer.camOrthoRect.overlaps( rect );
		}

		private ScreenUtils() {
		}
	}

	public GameRenderer( GameWorld gameWorld, ScalingStrategy scalingStrategy ) {
		world = gameWorld;
		gl = Gdx.graphics.getGL20();

		int width = Gdx.graphics.getWidth();
		int height = Gdx.graphics.getHeight();

		// world rendering
		worldRenderer = new GameWorldRenderer( scalingStrategy, world, width, height );
		batchRenderer = new GameBatchRenderer( gl );

		// initialize utils
		ScreenUtils.init( worldRenderer );
		Gdx.app.log( "GameRenderer", "ScreenUtils " + (ScreenUtils.ready ? "initialized." : "NOT initialized!") );

		// post-processing
		postProcessor = new PostProcessor( width, height, true /* depth */, false /* alpha */, Config.isDesktop /* supports32Bpp */);
	}

	public void dispose() {
		postProcessor.dispose();

		// depthMap.dispose();
		batchRenderer.dispose();

		GameEvents.gameRenderer.removeAllListeners();
	}

	public PostProcessor getPostProcessor() {
		return postProcessor;
	}

	public GameWorldRenderer getWorldRenderer() {
		return worldRenderer;
	}

	public void beforeRender( float timeAliasingFactor ) {
		GameEvents.gameRenderer.timeAliasingFactor = timeAliasingFactor;
		GameEvents.gameRenderer.trigger( this, GameRendererEvent.Type.OnSubframeInterpolate );
	}

	public void render( FrameBuffer dest ) {
		// postproc begins
		boolean postProcessorReady = false;
		boolean hasDest = (dest != null);

		if( postProcessor.isEnabled() ) {
			postProcessorReady = postProcessor.capture();
		}

		if( !postProcessorReady ) {
			if( hasDest ) {
				dest.begin();
			} else {
				gl.glViewport( 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );
			}

			gl.glClearDepthf( 1 );
			gl.glClearColor( 0, 0, 0, 0 );
			gl.glClear( GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT );
		}
		// postproc ends

		gl.glDepthMask( true );

		// render base tilemap
		worldRenderer.renderTilemap();

		// BatchBeforeMeshes
		SpriteBatch batch = null;
		batch = batchRenderer.begin( worldRenderer.getOrthographicCamera() );
		batch.enableBlending();
		{
			GameEvents.gameRenderer.batch = batch;
			GameEvents.gameRenderer.trigger( this, GameRendererEvent.Type.BatchBeforeMeshes );
		}
		batchRenderer.end();

		// render world's meshes
		worldRenderer.renderAllMeshes( false );

		// BatchAfterMeshes
		batch = batchRenderer.beginTopLeft();
		{
			GameEvents.gameRenderer.batch = batch;
			GameEvents.gameRenderer.trigger( this, GameRendererEvent.Type.BatchAfterMeshes );
		}
		batchRenderer.end();

		// postproc begins
		if( postProcessorReady ) {
			gl.glDisable( GL20.GL_CULL_FACE );
			if( world.isNightMode() ) {
				FrameBuffer result = postProcessor.captureEnd();
				worldRenderer.renderLigthMap( result );
			}
			postProcessor.render( dest );
		} else {
			if( hasDest ) {
				dest.end();
			}

			if( world.isNightMode() ) {
				worldRenderer.renderLigthMap( dest );
			}
		}
		// postproc ends
	}

	// manages and triggers debug event
	public void debugRender() {
		SpriteBatch batch = batchRenderer.beginTopLeft();
		batch.disableBlending();
		GameEvents.gameRenderer.batch = batch;
		GameEvents.gameRenderer.trigger( this, GameRendererEvent.Type.BatchDebug );
		batchRenderer.end();

		GameEvents.gameRenderer.batch = null;
		GameEvents.gameRenderer.trigger( this, GameRendererEvent.Type.Debug );
	}

	public void rebind() {
		postProcessor.rebind();
	}
}
