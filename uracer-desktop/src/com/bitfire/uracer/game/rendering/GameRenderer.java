package com.bitfire.uracer.game.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.game.events.GameEvents;
import com.bitfire.uracer.game.player.PlayerCar;
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

	/** Manages to convert world positions expressed in meters or pixels to screen pixels.
	 * To use this class, the GameWorldRenderer MUST be already constructed and initialized. */
	public static final class ScreenUtils {
		private static Vector2 screenPosFor = new Vector2();
		private static GameWorldRenderer worldRenderer;

		public static void init( GameWorldRenderer worldRenderer ) {
			ScreenUtils.worldRenderer = worldRenderer;
		}

		public static Vector2 screenPosForMt( Vector2 worldPositionMt ) {
			screenPosFor.x = Convert.mt2px( worldPositionMt.x ) - worldRenderer.camOrtho.position.x + worldRenderer.halfViewport.x;
			screenPosFor.y = worldRenderer.camOrtho.position.y - Convert.mt2px( worldPositionMt.y ) + worldRenderer.halfViewport.y;
			return screenPosFor;
		}

		public static Vector2 screenPosForPx( Vector2 worldPositionPx ) {
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

	public GameRenderer( ScalingStrategy scalingStrategy, GameWorld gameWorld, boolean createPostProcessing ) {
		gl = Gdx.graphics.getGL20();
		world = gameWorld;

		int width = Gdx.graphics.getWidth();
		int height = Gdx.graphics.getHeight();

		// world rendering
		worldRenderer = new GameWorldRenderer( scalingStrategy, world, width, height );
		batchRenderer = new GameBatchRenderer( gl );

		// initialize utils
		ScreenUtils.init( worldRenderer );

		// post-processing
		boolean supports32Bpp = Config.isDesktop;
		postProcessor = (createPostProcessing ? new PostProcessor( width, height, false /* depth */, false /* alpha */, supports32Bpp ) : null);
	}

	public void dispose() {
		batchRenderer.dispose();

		if( hasPostProcessor() ) {
			postProcessor.dispose();
		}
	}

	public boolean hasPostProcessor() {
		return (postProcessor != null);
	}

	public PostProcessor getPostProcessor() {
		return postProcessor;
	}

	public GameWorldRenderer getWorldRenderer() {
		return worldRenderer;
	}

	public void onBeforeRender( PlayerCar player ) {
		worldRenderer.syncWithPlayer( player );
	}

	public void render( PlayerCar player ) {
		boolean postProcessorReady = hasPostProcessor() && postProcessor.isEnabled();
		if( postProcessorReady ) {
			postProcessorReady = postProcessor.capture();
		}

		if( !postProcessorReady ) {
			gl.glViewport( 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );
			gl.glClearDepthf( 1 );
			gl.glClearColor( 0, 0, 0, 0 );
			gl.glClear( GL20.GL_DEPTH_BUFFER_BIT | GL20.GL_COLOR_BUFFER_BIT );
		}

		// render base tilemap
		worldRenderer.renderTilemap( gl );

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
		worldRenderer.renderAllMeshes( gl );

		// BatchAfterMeshes
		batch = batchRenderer.beginTopLeft();
		{
			GameEvents.gameRenderer.batch = batch;
			GameEvents.gameRenderer.trigger( this, GameRendererEvent.Type.BatchAfterMeshes );
		}
		batchRenderer.end();

		if( world.isNightMode() ) {
			if( Config.Graphics.DumbNightMode ) {
				if( postProcessorReady ) {
					postProcessor.render();
				}

				worldRenderer.renderLigthMap( null );
			} else {
				// hook into the next PostProcessor source buffer (the last result)
				// and blend the lightmap on it
				if( postProcessorReady ) {
					worldRenderer.renderLigthMap( postProcessor.captureEnd() );
					postProcessor.render();
				} else {
					worldRenderer.renderLigthMap( null );
				}
			}
		} else {
			if( postProcessorReady ) {
				postProcessor.render();
			}
		}

		//
		// manages and triggers debug event
		//
		batch = batchRenderer.beginTopLeft();
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
