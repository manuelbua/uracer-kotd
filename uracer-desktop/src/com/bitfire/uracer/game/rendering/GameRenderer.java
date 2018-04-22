
package com.bitfire.uracer.game.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.bitfire.postprocessing.PostProcessor;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.events.GameRendererEvent;
import com.bitfire.uracer.game.logic.post.PostProcessing;
import com.bitfire.uracer.game.logic.post.PostProcessing.Effects;
import com.bitfire.uracer.game.logic.post.effects.Ssao;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.ScaleUtils;

/** Manages the high-level rendering of the whole world and triggers all the GameRendererEvent events associated with the rendering
 * timeline, realized with the event's renderqueue mechanism.
 *
 * @author bmanuel */
public final class GameRenderer {
	private final GL20 gl;
	private final GameWorld world;
	private final GameBatchRenderer batchRenderer;
	private final GameWorldRenderer worldRenderer;

	private PostProcessing postProcessing = null;
	private PostProcessor postProcessor = null;

	private final Matrix4 identity = new Matrix4();
	private final Matrix4 xform = new Matrix4();

	private boolean debug = false;

	public GameRenderer (GameWorld gameWorld) {
		world = gameWorld;
		gl = Gdx.graphics.getGL20();

		postProcessing = new PostProcessing(gameWorld);
		postProcessor = postProcessing.getPostProcessor();

		worldRenderer = new GameWorldRenderer(world, postProcessing.isEnabled());
		batchRenderer = new GameBatchRenderer(gl);

		if (postProcessing.isEnabled() && postProcessing.hasEffect(Effects.Ssao.name)) {
			Ssao ssao = (Ssao)postProcessing.getEffect(Effects.Ssao.name);
			ssao.setNormalDepthMap(worldRenderer.getNormalDepthMap().getColorBufferTexture());
		}

		// initialize utils
		ScreenUtils.init(worldRenderer);

		// precompute sprite batch transform matrix
		xform.idt();
		xform.scale(ScaleUtils.Scale, ScaleUtils.Scale, 1);
	}

	public void dispose () {
		postProcessing.dispose();
		batchRenderer.dispose();
		worldRenderer.dispose();

		GameEvents.gameRenderer.removeAllListeners();
	}

	/** Enables or disables the debug render events */
	public void setDebug (boolean enabled) {
		debug = enabled;
	}

	public boolean isDebugEnabled () {
		return debug;
	}

	public PostProcessing getPostProcessing () {
		return postProcessing;
	}

	public GameWorldRenderer getWorldRenderer () {
		return worldRenderer;
	}

	public FrameBuffer getNormalDepthMap () {
		return worldRenderer.getNormalDepthMap();
	}

	private void interpolate (float timeAliasingFactor) {
		GameEvents.gameRenderer.timeAliasingFactor = timeAliasingFactor;
		GameEvents.gameRenderer.trigger(this, GameRendererEvent.Type.SubframeInterpolate);
	}

	private void beforeRender () {
		// request freshdata before any rendering
		GameEvents.gameRenderer.trigger(this, GameRendererEvent.Type.BeforeRender);

		// update matrices, cameras and other values
		GameEvents.gameRenderer.mtxOrthographicMvpMt = worldRenderer.getOrthographicMvpMt();
		GameEvents.gameRenderer.camOrtho = worldRenderer.getOrthographicCamera();
		GameEvents.gameRenderer.camPersp = worldRenderer.getPerspectiveCamera();
		GameEvents.gameRenderer.camZoom = worldRenderer.getCameraZoom();
		GameEvents.gameRenderer.postProcessor = postProcessing.getPostProcessor();
	}

	private void clear () {
		gl.glClearDepthf(1);
		gl.glClearColor(0, 0, 0, 0);
		gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
	}

	public void render (FrameBuffer dest, boolean quitPending, boolean paused) {
		if (!quitPending) {
			if (!paused) {
				// trigger interpolables to interpolate their position and orientation
				interpolate(URacer.Game.getTemporalAliasing());
			}

			// raise before render
			beforeRender();
		} else {
			GameEvents.gameRenderer.trigger(this, GameRendererEvent.Type.BeforeRender);
		}

		SpriteBatch batch;
		worldRenderer.resetCounters();

		clear();

		if (postProcessing.requiresNormalDepthMap()) {
			worldRenderer.updateNormalDepthMap();
		}

		// postproc begins
		boolean postProcessorReady = false;
		boolean hasDest = (dest != null);

		if (postProcessor != null && postProcessor.isEnabled()) {
			postProcessorReady = postProcessor.capture();
		}

		if (!postProcessorReady) {
			if (hasDest) {
				dest.begin();
			} else {
				gl.glViewport(ScaleUtils.CropX, ScaleUtils.CropY, ScaleUtils.PlayWidth, ScaleUtils.PlayHeight);
			}

			clear();
		}
		// postproc ends

		gl.glDepthMask(true);

		// render base tilemap
		worldRenderer.renderTilemap();

		// ///////////////////////
		// BatchBeforeMeshes
		// ///////////////////////

		{
			batch = batchRenderer.begin(worldRenderer.getOrthographicCamera());
			batch.enableBlending();
			GameEvents.gameRenderer.batch = batch;
			GameEvents.gameRenderer.trigger(this, GameRendererEvent.Type.BatchBeforeCars);
			batchRenderer.end();
		}

		{
			gl.glEnable(GL20.GL_DEPTH_TEST);
			gl.glDepthFunc(GL20.GL_LESS);
			worldRenderer.renderCars(false);
		}

		{
			gl.glDisable(GL20.GL_DEPTH_TEST);
			gl.glDisable(GL20.GL_CULL_FACE);
			batch = batchRenderer.begin(worldRenderer.getOrthographicCamera());
			batch.enableBlending();
			{
				GameEvents.gameRenderer.batch = batch;
				GameEvents.gameRenderer.trigger(this, GameRendererEvent.Type.BatchAfterCars);
			}
			batchRenderer.end();
		}

		gl.glEnable(GL20.GL_DEPTH_TEST);
		gl.glDepthFunc(GL20.GL_LESS);
		worldRenderer.renderWalls(false);

		if (world.isNightMode()) {
			gl.glDisable(GL20.GL_DEPTH_TEST);
			if (postProcessorReady) {
				FrameBuffer result = postProcessor.captureEnd();
				worldRenderer.renderLigthMap(result);
				postProcessor.captureNoClear();
			} else {
				if (hasDest) dest.end();
				worldRenderer.renderLigthMap(dest);
				if (hasDest) dest.begin();
			}
		}

		gl.glEnable(GL20.GL_DEPTH_TEST);
		worldRenderer.renderTrees(false);
		gl.glDisable(GL20.GL_DEPTH_TEST);

		// ///////////////////////
		// BatchAfterMeshes
		// ///////////////////////

		batch = batchRenderer.beginTopLeft();
		batch.setTransformMatrix(xform);
		{
			GameEvents.gameRenderer.batch = batch;
			GameEvents.gameRenderer.trigger(this, GameRendererEvent.Type.BatchBeforePostProcessing);
		}
		batchRenderer.end();

		if (postProcessorReady) {
			postProcessor.render(dest);

			if (hasDest) dest.begin();
			batchAfterPostProcessing();
			debugRender();
			if (hasDest) dest.end();

		} else {
			batchAfterPostProcessing();
			debugRender();
			if (hasDest) dest.end();
		}
	}

	private void batchAfterPostProcessing () {
		SpriteBatch batch = batchRenderer.beginTopLeft();
		batch.setTransformMatrix(xform);

		GameEvents.gameRenderer.batch = batch;
		GameEvents.gameRenderer.trigger(this, GameRendererEvent.Type.BatchAfterPostProcessing);
		batchRenderer.end();
	}

	// manages and triggers debug event
	public void debugRender () {
		if (debug) {
			SpriteBatch batch = batchRenderer.beginTopLeft();

			batch.setTransformMatrix(xform);
			batch.disableBlending();
			GameEvents.gameRenderer.batch = batch;
			GameEvents.gameRenderer.trigger(this, GameRendererEvent.Type.BatchDebug);
			batchRenderer.end();

			batch.setTransformMatrix(identity);
			GameEvents.gameRenderer.batch = null;
			GameEvents.gameRenderer.trigger(this, GameRendererEvent.Type.Debug);
		}
	}

	public void rebind () {
		if (postProcessor != null && postProcessor.isEnabled()) {
			postProcessor.rebind();
		}
	}

	/** Manages to convert world positions expressed in meters or pixels to the corresponding position to screen pixels. To use this
	 * class, the GameWorldRenderer MUST be already constructed and initialized. */
	public static final class ScreenUtils {
		// private static int ScreenWidth, ScreenHeight;
		private static Vector2 tmp2 = new Vector2();
		private static Vector3 tmp3 = new Vector3();
		private static GameWorldRenderer worldRenderer;

		// private static Vector2 ref2scr, scr2ref;

		public static void init (GameWorldRenderer worldRenderer) {
			ScreenUtils.worldRenderer = worldRenderer;
			// ScreenUtils.ScreenWidth = Gdx.graphics.getWidth();
			// ScreenUtils.ScreenHeight = Gdx.graphics.getHeight();
		}

		/** Transforms Box2D world-mt coordinates to reference-screen pixels coordinates */
		public static Vector2 worldMtToScreen (Vector2 worldPositionMt) {
			return worldPxToScreen(Convert.mt2px(worldPositionMt));
		}

		/** Transforms world-px coordinates to reference-screen pixel coordinates */
		public static Vector2 worldPxToScreen (Vector2 worldPositionPx) {
			tmp3.set(worldPositionPx.x, worldPositionPx.y, 0);
			worldRenderer.camOrtho.project(tmp3, 0, 0, Config.Graphics.ReferenceScreenWidth, Config.Graphics.ReferenceScreenHeight);
			tmp2.set(tmp3.x, Config.Graphics.ReferenceScreenHeight - tmp3.y);
			return tmp2;
		}

		// /** Transforms reference-screen pixel coordinates to world-mt coordinates */
		// public static Vector3 screenToWorldMt (Vector2 screenPositionPx) {
		// tmp3.set(screenPositionPx.x, screenPositionPx.y, 1);
		//
		// // normalize and scale to the real display size
		// tmp3.x = (tmp3.x / (float)Config.Graphics.ReferenceScreenWidth) * ScreenWidth;
		// tmp3.y = (tmp3.y / (float)Config.Graphics.ReferenceScreenHeight) * ScreenHeight;
		//
		// worldRenderer.camOrtho.unproject(tmp3, 0, 0, ScreenWidth, ScreenHeight);
		//
		// tmp2.set(Convert.px2mt(tmp3.x), Convert.px2mt(tmp3.y));
		// tmp3.set(tmp2.x, tmp2.y, 0);
		// return tmp3;
		// }

		public static boolean isVisible (Rectangle rect) {
			return worldRenderer.camOrthoRect.overlaps(rect);
		}

		private ScreenUtils () {
		}
	}
}
