
package com.bitfire.uracer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.postprocessing.PostProcessor;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.events.GameRendererEvent;
import com.bitfire.uracer.game.events.GameRendererEvent.Order;
import com.bitfire.uracer.game.events.GameRendererEvent.Type;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.rendering.GameWorldRenderer;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.NumberString;
import com.bitfire.uracer.utils.ScaleUtils;
import com.bitfire.uracer.utils.SpriteBatchUtils;

public final class DebugHelper {

	// rendering
	private GameWorldRenderer worldRenderer;
	private PostProcessor postProcessor;
	private Matrix4 idt = new Matrix4();

	// player
	// private static PlayerCar player;

	// frame stats
	private DebugStatistics stats;
	private String uRacerInfo;

	// box2d
	private Box2DDebugRenderer b2drenderer;
	private World box2dWorld;

	private final GameRendererEvent.Listener renderListener = new GameRendererEvent.Listener() {
		@Override
		public void handle (Object source, Type type, Order order) {
			if (type == Type.BatchDebug) {
				render(GameEvents.gameRenderer.batch);
			} else if (type == Type.Debug) {
				if (Config.Debug.RenderBox2DWorldWireframe) {
					renderB2dWorld(box2dWorld, worldRenderer.getOrthographicMvpMt());
				}
			}
		}
	};

	public DebugHelper (GameWorldRenderer worldRenderer, World box2dWorld, PostProcessor postProcessor) {
		this.worldRenderer = worldRenderer;
		this.box2dWorld = box2dWorld;
		this.postProcessor = postProcessor;

		GameEvents.gameRenderer.addListener(renderListener, GameRendererEvent.Type.BatchDebug, GameRendererEvent.Order.DEFAULT);
		GameEvents.gameRenderer.addListener(renderListener, GameRendererEvent.Type.Debug, GameRendererEvent.Order.DEFAULT);

		// player = null;
		b2drenderer = new Box2DDebugRenderer();

		// extrapolate version information
		uRacerInfo = "URacer " + URacer.versionInfo;

		// compute graphics stats size
		float updateHz = 60f;
		if (!URacer.Game.isDesktop()) {
			updateHz = 5f;
		}

		int sw = MathUtils.clamp(uRacerInfo.length() * Art.DebugFontWidth, 100, 500);
		stats = new DebugStatistics(sw, 100, updateHz);
	}

	public void dispose () {
		GameEvents.gameRenderer.removeListener(renderListener, GameRendererEvent.Type.BatchDebug, GameRendererEvent.Order.DEFAULT);
		GameEvents.gameRenderer.removeListener(renderListener, GameRendererEvent.Type.Debug, GameRendererEvent.Order.DEFAULT);

		b2drenderer.dispose();
		stats.dispose();
	}

	public static void setPlayer (PlayerCar player) {
		// DebugHelper.player = player;
	}

	public void update () {
		stats.update();
	}

	private void render (SpriteBatch batch) {
		batch.setTransformMatrix(idt);

		renderVersionInfo(batch, Art.DebugFontHeight * 2);

		if (Config.Debug.RenderDebugInfoFpsStats) {
			renderFpsStats(batch, ScaleUtils.PlayHeight - Art.DebugFontHeight);
		}

		if (Config.Debug.ShowAdvancedDebugInfo) {
			if (Config.Debug.RenderDebugInfoGraphics) {
				renderGraphicalStats(batch, Art.DebugFontHeight * 2);
			}

			if (Config.Debug.RenderDebugInfoMemoryStats) {
				renderMemoryUsage(batch, ScaleUtils.PlayHeight - Art.DebugFontHeight * 4);
			}

			// if (Config.Debug.RenderPlayerDebugInfo && player != null) {
			// renderPlayerInfo(batch, player, 0);
			// }

			if (Config.Debug.RenderDebugInfoPostProcessor && postProcessor != null) {
				renderPostProcessorInfo(batch, postProcessor, ScaleUtils.PlayHeight - Art.DebugFontHeight);
			}

			if (Config.Debug.RenderDebugInfoMeshStats) {
				SpriteBatchUtils.drawString(batch, "total meshes=" + GameWorld.TotalMeshes, 0, ScaleUtils.PlayHeight
					- Art.DebugFontHeight * 3);
				SpriteBatchUtils.drawString(batch, "rendered meshes="
					+ (GameWorldRenderer.renderedTrees + GameWorldRenderer.renderedWalls) + ", trees="
					+ GameWorldRenderer.renderedTrees + ", walls=" + GameWorldRenderer.renderedWalls + ", culled="
					+ GameWorldRenderer.culledMeshes, 0, ScaleUtils.PlayHeight - Art.DebugFontHeight * 2);
			}
		}
	}

	private void renderGraphicalStats (SpriteBatch batch, int y) {
		batch.enableBlending();
		batch.setColor(1, 1, 1, 0.8f);
		batch.draw(stats.getRegion(), ScaleUtils.PlayWidth - stats.getWidth(), y);
		batch.setColor(1, 1, 1, 1f);
		batch.disableBlending();
	}

	private void renderFpsStats (SpriteBatch batch, int y) {
		String text = "fps: " + NumberString.formatLong(Gdx.graphics.getFramesPerSecond()) + ", phy: "
			+ NumberString.formatLong(stats.meanPhysics.getMean()) + ", gfx: " + NumberString.formatLong(stats.meanRender.getMean())
			+ ", ticks: " + NumberString.formatLong(stats.meanTickCount.getMean());

		SpriteBatchUtils.drawString(batch, text, ScaleUtils.PlayWidth - text.length() * Art.DebugFontWidth, y);
	}

	private void renderVersionInfo (SpriteBatch batch, int y) {
		SpriteBatchUtils.drawString(batch, uRacerInfo, ScaleUtils.PlayWidth - uRacerInfo.length() * Art.DebugFontWidth, 0,
			Art.DebugFontWidth, y);
	}

	private void renderMemoryUsage (SpriteBatch batch, int y) {
		float oneOnMb = 1f / 1048576f;
		float javaHeapMb = (float)Gdx.app.getJavaHeap() * oneOnMb;
		float nativeHeapMb = (float)Gdx.app.getNativeHeap() * oneOnMb;

		String text = "java heap = " + NumberString.format(javaHeapMb) + "MB" + " - native heap = "
			+ NumberString.format(nativeHeapMb) + "MB";

		SpriteBatchUtils.drawString(batch, text, 0, y);
	}

	private void renderB2dWorld (World world, Matrix4 modelViewProj) {
		b2drenderer.render(world, modelViewProj);
	}

	private void renderPostProcessorInfo (SpriteBatch batch, PostProcessor postProcessor, int y) {
		String text = "Post-processing fx count = " + postProcessor.getEnabledEffectsCount();
		SpriteBatchUtils.drawString(batch, text, 0, y);
	}

	// private void renderPlayerInfo (SpriteBatch batch, PlayerCar player, int y) {
	// CarDescriptor carDesc = player.getCarDescriptor();
	// Body body = player.getBody();
	// Vector2 pos = GameRenderer.ScreenUtils.worldMtToScreen(body.getPosition());
	// EntityRenderState state = player.state();
	//
	// SpriteBatchUtils.drawString(batch, "vel_wc len =" + carDesc.velocity_wc.len(), 0, y);
	// SpriteBatchUtils.drawString(batch, "vel_wc [x=" + carDesc.velocity_wc.x + ", y=" + carDesc.velocity_wc.y + "]", 0, y
	// + Art.DebugFontWidth);
	// SpriteBatchUtils.drawString(batch, "steerangle=" + carDesc.steerangle, 0, y + Art.DebugFontWidth * 2);
	// SpriteBatchUtils.drawString(batch, "throttle=" + carDesc.throttle, 0, y + Art.DebugFontWidth * 3);
	// SpriteBatchUtils.drawString(batch, "screen x=" + pos.x + ",y=" + pos.y, 0, y + Art.DebugFontWidth * 4);
	// SpriteBatchUtils.drawString(batch, "world-mt x=" + body.getPosition().x + ",y=" + body.getPosition().y, 0, y
	// + Art.DebugFontWidth * 5);
	// SpriteBatchUtils.drawString(batch,
	// "world-px x=" + Convert.mt2px(body.getPosition().x) + ",y=" + Convert.mt2px(body.getPosition().y), 0, y
	// + Art.DebugFontWidth * 6);
	// // Debug.drawString( "dir worldsize x=" + Director.worldSizeScaledPx.x + ",y=" +
	// // Director.worldSizeScaledPx.y, 0, 100 );
	// // Debug.drawString( "dir bounds x=" + Director.boundsPx.x + ",y=" + Director.boundsPx.width, 0, 107 );
	// SpriteBatchUtils.drawString(batch, "orient=" + body.getAngle(), 0, y + Art.DebugFontWidth * 7);
	// SpriteBatchUtils.drawString(batch, "render.interp=" + (state.position.x + "," + state.position.y), 0, y
	// + Art.DebugFontWidth * 8);
	//
	// // BatchUtils.drawString( batch, "on tile " + tilePosition, 0, 0 );
	// }
}
