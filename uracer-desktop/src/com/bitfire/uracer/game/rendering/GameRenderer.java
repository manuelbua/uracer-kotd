package com.bitfire.uracer.game.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.game.actors.PlayerCar;
import com.bitfire.uracer.game.events.GameEvents;
import com.bitfire.uracer.game.events.GameRendererEvent;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.postprocessing.PostProcessor;

public class GameRenderer {
	private final GL20 gl;
	private final GameWorld world;
	private final GameBatchRenderer batchRenderer;
	private final GameWorldRenderer worldRenderer;
	private final PostProcessor postProcessor;
	private boolean postProcessorEnabled = Config.PostProcessing.Enabled;

	public GameRenderer( ScalingStrategy scalingStrategy, GameWorld gameWorld, World box2dWorld ) {
		gl = Gdx.graphics.getGL20();
		world = gameWorld;

		int width = Gdx.graphics.getWidth();
		int height = Gdx.graphics.getHeight();

		worldRenderer = new GameWorldRenderer( scalingStrategy, world, width, height );
		batchRenderer = new GameBatchRenderer( gl );
		postProcessor = new PostProcessor( width, height, false /* depth */, false /* alpha */, Config.isDesktop /* 32bpp */);

		Debug.create( box2dWorld );
	}

	public void dispose() {
		Debug.dispose();
		batchRenderer.dispose();
		postProcessor.dispose();
	}

	public void setPostProcessorEnabled( boolean enable ) {
		postProcessorEnabled = enable;
	}

	public PostProcessor getPostProcessor() {
		return postProcessor;
	}

	public void render( PlayerCar player ) {
		OrthographicCamera ortho = Director.getCamera();

		// resync
		worldRenderer.syncWithCam( ortho );

		if( postProcessorEnabled ) {
			postProcessor.capture();
		} else {
			gl.glViewport( 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );
			gl.glClearDepthf( 1 );
			gl.glClearColor( 0, 0, 0, 0 );
			gl.glClear( GL20.GL_DEPTH_BUFFER_BIT | GL20.GL_COLOR_BUFFER_BIT );
		}

		// render base tilemap
		worldRenderer.renderTilemap( gl );

		// BatchBeforeMeshes
		SpriteBatch batch = null;
		batch = batchRenderer.begin( ortho );
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
				if( postProcessorEnabled ) {
					postProcessor.render();
				}

				worldRenderer.generatePlayerHeadlightsLightMap( player );
				worldRenderer.renderLigthMap( null );
			} else {
				// render nightmode
				if( world.isNightMode() ) {
					worldRenderer.generatePlayerHeadlightsLightMap( player );

					// hook into the next PostProcessor source buffer (the last result)
					// and blend the lightmap on it
					if( postProcessorEnabled ) {
						worldRenderer.renderLigthMap( postProcessor.captureEnd() );
					} else {
						worldRenderer.renderLigthMap( null );
					}
				}

				if( postProcessorEnabled ) {
					postProcessor.render();
				}
			}
		} else {
			if( postProcessorEnabled ) {
				postProcessor.render();
			}
		}

		//
		// debug
		//

		batch = batchRenderer.beginTopLeft();
		GameEvents.gameRenderer.trigger( GameRendererEvent.Type.BatchDebug );
		batchRenderer.end();
	}

	public void rebind() {
		postProcessor.rebind();
	}
}
