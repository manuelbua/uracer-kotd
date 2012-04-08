package com.bitfire.uracer.game.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.debug.Debug;
import com.bitfire.uracer.effects.CarSkidMarks;
import com.bitfire.uracer.effects.TrackEffect;
import com.bitfire.uracer.events.GameRendererEvent;
import com.bitfire.uracer.game.GameData;
import com.bitfire.uracer.game.GameWorld;
import com.bitfire.uracer.postprocessing.PostProcessor;

public class GameRenderer {
	public static final GameRendererEvent event = new GameRendererEvent();

	private final GL20 gl;
	private final GameWorld world;
	private final GameBatchRenderer batchRenderer;
	private final GameWorldRenderer worldRenderer;
	public final PostProcessor postProcessor;
	private boolean postProcessorEnabled = Config.Graphics.EnablePostProcessingFx;

	public GameRenderer( GameWorld gameWorld ) {
		gl = Gdx.graphics.getGL20();
		world = gameWorld;

		int width = Gdx.graphics.getWidth();
		int height = Gdx.graphics.getHeight();
		worldRenderer = new GameWorldRenderer( world, width, height );
		batchRenderer = new GameBatchRenderer( gl );
		postProcessor = new PostProcessor( width, height, false /* depth */, false /* alpha */, Config.isDesktop /* 32
																												 * bits */);
	}

	public void dispose() {
		batchRenderer.dispose();
		postProcessor.dispose();
	}

	public void setPostProcessorEnabled( boolean enable ) {
		postProcessorEnabled = enable;
	}

	public void render() {
		OrthographicCamera ortho = Director.getCamera();

		// tweener step
		GameData.tweener.update( (int)(URacer.getLastDeltaSecs() * 1000) );

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
			event.batch = batch;
			event.trigger( GameRendererEvent.Type.BatchBeforeMeshes );
		}
		batchRenderer.end();

		// render world's meshes
		worldRenderer.renderAllMeshes( gl );

		// BatchAfterMeshes
		batch = batchRenderer.beginTopLeft();
		{
			event.batch = batch;
			event.trigger( GameRendererEvent.Type.BatchAfterMeshes );
		}
		batchRenderer.end();

		if( world.isNightMode() ) {
			if( Config.Graphics.DumbNightMode ) {
				if( postProcessorEnabled )
					postProcessor.render();

				worldRenderer.generateLightMap();
				worldRenderer.renderLigthMap( null );
			} else {
				// render nightmode
				if( world.isNightMode() ) {
					worldRenderer.generateLightMap();

					// hook into the next PostProcessor source buffer (the last result)
					// and blend the lightmap on it
					if( postProcessorEnabled ) {
						worldRenderer.renderLigthMap( postProcessor.captureEnd() );
					}
					else {
						worldRenderer.renderLigthMap( null );
					}
				}

				if( postProcessorEnabled )
					postProcessor.render();
			}
		} else {
			if( postProcessorEnabled )
				postProcessor.render();
		}


		//
		// debug
		//

		batch = batchRenderer.beginTopLeft();

		if( Config.isDesktop ) {
			if( Config.Graphics.RenderBox2DWorldWireframe ) {
				Debug.renderB2dWorld( GameData.b2dWorld, Director.getMatViewProjMt() );
			}

			// EntityManager.raiseOnDebug();
			event.trigger( GameRendererEvent.Type.BatchDebug );

			Debug.renderVersionInfo( batch );
			Debug.renderGraphicalStats( batch, Gdx.graphics.getWidth() - Debug.getStatsWidth(), Gdx.graphics.getHeight() - Debug.getStatsHeight() - Debug.fontHeight );
			Debug.renderTextualStats( batch );
			Debug.renderMemoryUsage( batch );
			Debug.drawString( batch, "Visible car skid marks=" + ((CarSkidMarks)GameData.System.trackEffects.get( TrackEffect.Type.CarSkidMarks )).getParticleCount(), 0,
					Gdx.graphics.getHeight() - 21 );
			Debug.drawString( batch, "total meshes=" + GameWorld.TotalMeshes, 0, Gdx.graphics.getHeight() - 14 );
			Debug.drawString( batch, "rendered meshes=" + (GameWorldRenderer.renderedTrees + GameWorldRenderer.renderedWalls) + ", trees="
					+ GameWorldRenderer.renderedTrees + ", walls=" + GameWorldRenderer.renderedWalls + ", culled=" + GameWorldRenderer.culledMeshes, 0,
					Gdx.graphics.getHeight() - 7 );

		} else {

			Debug.renderVersionInfo( batch );
			Debug.renderTextualStats( batch );
		}

		batchRenderer.end();
	}

	public void rebind() {
		postProcessor.rebind();
	}
}
