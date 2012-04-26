package com.bitfire.uracer.game.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.game.events.GameEvents;
import com.bitfire.uracer.game.events.GameRendererEvent;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.postprocessing.PostProcessor;

public class GameRenderer {
	private final GL20 gl;
	private final GameWorld world;
	private final GameBatchRenderer batchRenderer;
	private final GameWorldRenderer worldRenderer;
	private final PostProcessor postProcessor;

	public GameRenderer( ScalingStrategy scalingStrategy, GameWorld gameWorld, boolean createPostProcessing ) {
		gl = Gdx.graphics.getGL20();
		world = gameWorld;

		int width = Gdx.graphics.getWidth();
		int height = Gdx.graphics.getHeight();

		// world rendering
		worldRenderer = new GameWorldRenderer( scalingStrategy, world, width, height );
		batchRenderer = new GameBatchRenderer( gl );

		// post-processing
		boolean supports32Bpp = Config.isDesktop;
		postProcessor = (createPostProcessing ? new PostProcessor( width, height, false /* depth */, false /* alpha */, supports32Bpp ) : null);
	}

	public void dispose() {
		batchRenderer.dispose();
		postProcessor.dispose();
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
		// resync
		worldRenderer.syncWithCam( Director.getCamera() );

		worldRenderer.updateRayHandler( Director.getMatViewProjMt(), player );
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
		batch = batchRenderer.begin( Director.getCamera() );
		{
			GameEvents.gameRenderer.batch = batch;
			GameEvents.gameRenderer.trigger( GameRendererEvent.Type.BatchBeforeMeshes );
		}
		batchRenderer.end();

		// render world's meshes
		worldRenderer.renderAllMeshes( gl );

		// BatchAfterMeshes
		batch = batchRenderer.beginTopLeft();
		{
			GameEvents.gameRenderer.batch = batch;
			GameEvents.gameRenderer.trigger( GameRendererEvent.Type.BatchAfterMeshes );
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
		// debug
		//

		GameEvents.gameRenderer.batch = batchRenderer.beginTopLeft();
		GameEvents.gameRenderer.trigger( GameRendererEvent.Type.BatchDebug );
		batchRenderer.end();
	}

	public void rebind() {
		postProcessor.rebind();
	}
}
