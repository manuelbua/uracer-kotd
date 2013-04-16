
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
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.configuration.UserPreferences;
import com.bitfire.uracer.configuration.UserPreferences.Preference;
import com.bitfire.uracer.events.GameRendererEvent;
import com.bitfire.uracer.game.GameEvents;
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
	private final PostProcessor postProcessor;
	private final GameWorldRenderer worldRenderer;
	private boolean drawNormalDepthMap;

	private final Matrix4 identity = new Matrix4();
	private final Matrix4 xform = new Matrix4();

	public GameRenderer (GameWorld gameWorld, ScalingStrategy scalingStrategy) {
		world = gameWorld;
		gl = Gdx.graphics.getGL20();

		gl.glViewport(ScaleUtils.CropX, ScaleUtils.CropY, ScaleUtils.PlayWidth, ScaleUtils.PlayHeight);

		// world rendering
		worldRenderer = new GameWorldRenderer(scalingStrategy, world);
		batchRenderer = new GameBatchRenderer(gl);

		// initialize utils
		ScreenUtils.init(worldRenderer, (int)scalingStrategy.referenceResolution.x, (int)scalingStrategy.referenceResolution.y);

		xform.idt();
		xform.scale(ScaleUtils.Scale, ScaleUtils.Scale, 1);

		// post-processing
		if (UserPreferences.bool(Preference.PostProcessing)) {
			postProcessor = new PostProcessor(ScaleUtils.PlayWidth, ScaleUtils.PlayHeight, true /* depth */, false /* alpha */,
				Config.isDesktop /* supports32Bpp */);
			PostProcessor.EnableQueryStates = false;
			postProcessor.setClearBits(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
			postProcessor.setClearColor(0, 0, 0, 1);
			postProcessor.setClearDepth(1);
		} else {
			postProcessor = null;
		}

		// needed deferred data
		drawNormalDepthMap = false;
	}

	public void enableNormalDepthMap () {
		drawNormalDepthMap = true;
	}

	public void disableNormalDepthMap () {
		drawNormalDepthMap = false;
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
		GameEvents.gameRenderer.mtxOrthographicMvpMt = worldRenderer.getOrthographicMvpMt();
		GameEvents.gameRenderer.camOrtho = worldRenderer.getOrthographicCamera();
		GameEvents.gameRenderer.camPersp = worldRenderer.getPerspectiveCamera();

		GameEvents.gameRenderer.timeAliasingFactor = timeAliasingFactor;
		GameEvents.gameRenderer.trigger(this, GameRendererEvent.Type.OnSubframeInterpolate);
	}

	public void render (FrameBuffer dest) {
		worldRenderer.resetCounters();

		if (drawNormalDepthMap) {
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
			}

			gl.glClearDepthf(1);
			gl.glClearColor(0, 0, 0, 1);
			gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		}
		// postproc ends

		gl.glDepthMask(true);

		// render base tilemap
		worldRenderer.renderTilemap();

		// ///////////////////////
		// BatchBeforeMeshes
		// ///////////////////////

		SpriteBatch batch = batchRenderer.begin(worldRenderer.getOrthographicCamera());
		batch.enableBlending();
		{
			GameEvents.gameRenderer.batch = batch;
			GameEvents.gameRenderer.trigger(this, GameRendererEvent.Type.BatchBeforeMeshes);
		}
		batchRenderer.end();

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
			gl.glEnable(GL20.GL_DEPTH_TEST);
		}

		worldRenderer.renderTrees(false);

		gl.glDisable(GL20.GL_DEPTH_TEST);

		// ///////////////////////
		// BatchAfterMeshes
		// ///////////////////////

		batch = batchRenderer.beginTopLeft();
		batch.setTransformMatrix(xform);
		{
			GameEvents.gameRenderer.batch = batch;
			GameEvents.gameRenderer.trigger(this, GameRendererEvent.Type.BatchAfterMeshes);
		}
		batchRenderer.end();

		if (postProcessorReady) {
			postProcessor.render(dest);

			if (hasDest) dest.begin();
			batchAfterPostProcessing();
			if (hasDest) dest.end();

		} else {
			batchAfterPostProcessing();
			if (hasDest) dest.end();
		}
	}

	private void batchAfterPostProcessing () {
		SpriteBatch batch = batchRenderer.beginTopLeft();
		batch.setTransformMatrix(xform);

		// BatchAfterPostProcessing
		GameEvents.gameRenderer.batch = batch;
		GameEvents.gameRenderer.trigger(this, GameRendererEvent.Type.BatchAfterPostProcessing);
		batchRenderer.end();
	}

	// manages and triggers debug event
	public void debugRender () {
		SpriteBatch batch = batchRenderer.beginTopLeft();
		batch.setTransformMatrix(xform);

		// batch.disableBlending();
		GameEvents.gameRenderer.batch = batch;
		GameEvents.gameRenderer.trigger(this, GameRendererEvent.Type.BatchDebug);
		batchRenderer.end();
		batch.setTransformMatrix(identity);

		GameEvents.gameRenderer.batch = null;
		GameEvents.gameRenderer.trigger(this, GameRendererEvent.Type.Debug);
	}

	public void rebind () {
		if (postProcessor != null && postProcessor.isEnabled()) {
			postProcessor.rebind();
		}
	}

	/** Manages to convert world positions expressed in meters or pixels to the corresponding position to screen pixels. To use this
	 * class, the GameWorldRenderer MUST be already constructed and initialized. */
	public static final class ScreenUtils {
		private static int RefScreenWidth, RefScreenHeight;
		private static int ScreenWidth, ScreenHeight;
		private static Vector2 tmp2 = new Vector2();
		private static Vector3 tmp3 = new Vector3();
		private static GameWorldRenderer worldRenderer;
		private static Vector2 ref2scr, scr2ref;

		public static void init (GameWorldRenderer worldRenderer, int width, int height) {
			ScreenUtils.worldRenderer = worldRenderer;
			ScreenUtils.RefScreenWidth = width;
			ScreenUtils.RefScreenHeight = height;
			ScreenUtils.ScreenWidth = Gdx.graphics.getWidth();
			ScreenUtils.ScreenHeight = Gdx.graphics.getHeight();

			// screen-type conversion ratios
			ref2scr = new Vector2((float)ScreenWidth / (float)RefScreenWidth, (float)ScreenHeight / (float)RefScreenHeight);
			scr2ref = new Vector2((float)RefScreenWidth / (float)ScreenWidth, (float)RefScreenHeight / (float)ScreenHeight);
		}

		/** Transforms Box2D world-mt coordinates to reference-screen pixels coordinates */
		public static Vector2 worldMtToRefScreen (Vector2 worldPositionMt) {
			return worldPxToRefScreen(Convert.mt2px(worldPositionMt));
		}

		/** Transforms world-px coordinates to reference-screen pixel coordinates */
		public static Vector2 worldPxToRefScreen (Vector2 worldPositionPx) {
			tmp3.set(worldPositionPx.x, worldPositionPx.y, 0);
			worldRenderer.camOrtho.project(tmp3, 0, 0, RefScreenWidth, RefScreenHeight);
			tmp2.set(tmp3.x, RefScreenHeight - tmp3.y);
			return tmp2;
		}

		public static Vector2 worldPxToScreen (Vector2 worldPositionPx) {
			Vector2 r = worldPxToRefScreen(worldPositionPx);
			r.scl(ref2scr);
			return r;
		}

		public static Vector2 worldMtToScreen (Vector2 worldPositionMt) {
			Vector2 r = worldMtToRefScreen(worldPositionMt);
			r.scl(ref2scr);
			return r;
		}

		/** Transforms reference-screen pixel coordinates to world-mt coordinates */
		public static Vector3 screenRefToWorldMt (Vector2 screenPositionPx) {
			tmp3.set(screenPositionPx.x, screenPositionPx.y, 1);

			// normalize and scale to the real display size
			tmp3.x = (tmp3.x / RefScreenWidth) * ScreenWidth;
			tmp3.y = (tmp3.y / RefScreenHeight) * ScreenHeight;

			worldRenderer.camOrtho.unproject(tmp3, 0, 0, ScreenWidth, ScreenHeight);

			tmp2.set(Convert.px2mt(tmp3.x), Convert.px2mt(tmp3.y));
			tmp3.set(tmp2.x, tmp2.y, 0);
			return tmp3;
		}

		public static boolean isVisible (Rectangle rect) {
			return worldRenderer.camOrthoRect.overlaps(rect);
		}

		private ScreenUtils () {
		}
	}
}
