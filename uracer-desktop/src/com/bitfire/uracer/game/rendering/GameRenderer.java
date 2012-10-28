
package com.bitfire.uracer.game.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.bitfire.postprocessing.PostProcessor;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.configuration.UserPreferences;
import com.bitfire.uracer.configuration.UserPreferences.Preference;
import com.bitfire.uracer.events.GameRendererEvent;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.utils.Convert;

/** Manages the high-level rendering of the whole world and triggers all the GameRendererEvent events associated with the rendering
 * timeline, realized with the event's renderqueue mechanism.
 * 
 * @author bmanuel */
public final class GameRenderer {
	private final GL20 gl;
	private final GameWorld world;
	private final GameBatchRenderer batchRenderer;
	private final PostProcessor postProcessor;
	private final GameWorldRenderer worldRenderer;

	/** Manages to convert world positions expressed in meters or pixels to the corresponding position to screen pixels. To use this
	 * class, the GameWorldRenderer MUST be already constructed and initialized. */
	public static final class ScreenUtils {
		public static int ScreenWidth, ScreenHeight;
		public static boolean ready = false;
		private static Vector2 screenPosFor = new Vector2();
		private static GameWorldRenderer worldRenderer;

		public static void init (GameWorldRenderer worldRenderer) {
			ScreenUtils.worldRenderer = worldRenderer;
			ScreenUtils.ready = true;
			ScreenUtils.ScreenWidth = Gdx.graphics.getWidth();
			ScreenUtils.ScreenHeight = Gdx.graphics.getHeight();
		}

		private static Vector3 vtmp = new Vector3();

		public static Vector2 worldMtToScreen (Vector2 worldPositionMt) {
			return worldPxToScreen(Convert.mt2px(worldPositionMt));
		}

		public static Vector2 worldPxToScreen (Vector2 worldPositionPx) {
			vtmp.set(worldPositionPx.x, worldPositionPx.y, 0);
			worldRenderer.camOrtho.project(vtmp, 0, 0, ScreenWidth, ScreenHeight);
			screenPosFor.set(vtmp.x, Gdx.graphics.getHeight() - vtmp.y);
			return screenPosFor;
		}

		public static boolean isVisible (Rectangle rect) {
			return worldRenderer.camOrthoRect.overlaps(rect);
		}

		private ScreenUtils () {
		}
	}

	public GameRenderer (GameWorld gameWorld, ScalingStrategy scalingStrategy) {
		world = gameWorld;
		gl = Gdx.graphics.getGL20();

		int width = Gdx.graphics.getWidth();
		int height = Gdx.graphics.getHeight();

		// world rendering
		worldRenderer = new GameWorldRenderer(scalingStrategy, world, width, height);
		batchRenderer = new GameBatchRenderer(gl);

		// initialize utils
		ScreenUtils.init(worldRenderer);
		Gdx.app.debug("GameRenderer", "ScreenUtils " + (ScreenUtils.ready ? "initialized." : "NOT initialized!"));

		// post-processing
		if (UserPreferences.bool(Preference.PostProcessing)) {
			postProcessor = new PostProcessor(width, height, true /* depth */, false /* alpha */, Config.isDesktop /* supports32Bpp */);
			PostProcessor.EnableQueryStates = false;
			postProcessor.setClearBits(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
			postProcessor.setClearColor(0, 0, 0, 1);
			postProcessor.setClearDepth(1);
		} else {
			postProcessor = null;
		}
	}

	public void dispose () {
		if (UserPreferences.bool(Preference.PostProcessing)) {
			postProcessor.dispose();
		}

		// depthMap.dispose();
		batchRenderer.dispose();
		worldRenderer.dispose();

		GameEvents.gameRenderer.removeAllListeners();
	}

	public boolean hasPostProcessor () {
		return postProcessor != null;
	}

	public PostProcessor getPostProcessor () {
		return postProcessor;
	}

	public GameWorldRenderer getWorldRenderer () {
		return worldRenderer;
	}

	public FrameBuffer getNormalDepthMap () {
		return worldRenderer.getNormalDepthMap();
	}

	public void beforeRender (float timeAliasingFactor) {
		gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		GameEvents.gameRenderer.timeAliasingFactor = timeAliasingFactor;
		GameEvents.gameRenderer.trigger(this, GameRendererEvent.Type.OnSubframeInterpolate);
	}

	public void render (FrameBuffer dest) {
		GameEvents.gameRenderer.mtxOrthographicMvpMt = worldRenderer.getOrthographicMvpMt();
		GameEvents.gameRenderer.camOrtho = worldRenderer.getOrthographicCamera();
		GameEvents.gameRenderer.camPersp = worldRenderer.getPerspectiveCamera();

		worldRenderer.updateNormalDepthMap();

		// postproc begins
		boolean postProcessorReady = false;
		boolean hasDest = (dest != null);

		if (postProcessor != null && postProcessor.isEnabled()) {
			postProcessorReady = postProcessor.capture();
		}

		if (!postProcessorReady) {
			if (hasDest) {
				dest.begin();
			}
			// else {
			// gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			// }

			gl.glClearDepthf(1);
			gl.glClearColor(0, 0, 0, 1);
			gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		}
		// postproc ends

		gl.glDepthMask(true);

		// render base tilemap
		worldRenderer.renderTilemap();

		// BatchBeforeMeshes
		SpriteBatch batch = null;
		batch = batchRenderer.begin(worldRenderer.getOrthographicCamera());
		batch.enableBlending();
		{
			GameEvents.gameRenderer.batch = batch;
			GameEvents.gameRenderer.trigger(this, GameRendererEvent.Type.BatchBeforeMeshes);
		}
		batchRenderer.end();

		// render world's meshes
		worldRenderer.renderAllMeshes(false);

		// BatchAfterMeshes
		batch = batchRenderer.beginTopLeft();
		{
			GameEvents.gameRenderer.batch = batch;
			GameEvents.gameRenderer.trigger(this, GameRendererEvent.Type.BatchAfterMeshes);
		}
		batchRenderer.end();

		// postproc begins
		if (postProcessorReady) {
			gl.glDisable(GL20.GL_CULL_FACE);
			if (world.isNightMode()) {
				FrameBuffer result = postProcessor.captureEnd();
				worldRenderer.renderLigthMap(result);
			}

			postProcessor.render(dest);

			if (hasDest) {
				dest.begin();
			}

			batchAfterPostProcessing();

			if (hasDest) {
				dest.end();
			}
		} else {
			batchAfterPostProcessing();
			if (hasDest) {
				dest.end();
			}

			if (world.isNightMode()) {
				worldRenderer.renderLigthMap(dest);
			}
		}
		// postproc ends
	}

	private void batchAfterPostProcessing () {
		// BatchAfterPostProcessing
		GameEvents.gameRenderer.batch = batchRenderer.beginTopLeft();
		GameEvents.gameRenderer.trigger(this, GameRendererEvent.Type.BatchAfterPostProcessing);
		batchRenderer.end();
	}

	// manages and triggers debug event
	public void debugRender () {
		SpriteBatch batch = batchRenderer.beginTopLeft();
		batch.disableBlending();
		GameEvents.gameRenderer.batch = batch;
		GameEvents.gameRenderer.trigger(this, GameRendererEvent.Type.BatchDebug);
		batchRenderer.end();

		// debug local normal+depth map
		Gdx.gl.glDisable(GL20.GL_BLEND);
		Gdx.gl.glDisable(GL20.GL_CULL_FACE);
		Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
		batch.begin();
		batch.disableBlending();
		float scale = 0.25f;
		int w = (int)(worldRenderer.getNormalDepthMap().getColorBufferTexture().getWidth() * scale);
		int h = (int)(worldRenderer.getNormalDepthMap().getColorBufferTexture().getHeight() * scale);
		int x = 10;
		int y = Gdx.graphics.getHeight() - h - 30;
		batch.draw(worldRenderer.getNormalDepthMap().getColorBufferTexture(), x, y, w, h);
		batch.end();

		GameEvents.gameRenderer.batch = null;
		GameEvents.gameRenderer.trigger(this, GameRendererEvent.Type.Debug);
	}

	public void rebind () {
		postProcessor.rebind();
	}
}
