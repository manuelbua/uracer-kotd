
package com.bitfire.uracer.game.debug;

import java.util.EnumSet;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.postprocessing.PostProcessor;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.entities.EntityRenderState;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.actors.CarDescriptor;
import com.bitfire.uracer.game.events.GameRendererEvent;
import com.bitfire.uracer.game.events.GameRendererEvent.Order;
import com.bitfire.uracer.game.events.GameRendererEvent.Type;
import com.bitfire.uracer.game.logic.gametasks.DisposableTasks;
import com.bitfire.uracer.game.logic.gametasks.GameTask;
import com.bitfire.uracer.game.logic.replaying.LapManager;
import com.bitfire.uracer.game.logic.replaying.Replay;
import com.bitfire.uracer.game.logic.replaying.ReplayManager.ReplayInfo;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.rendering.GameWorldRenderer;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.game.world.models.OrthographicAlignedStillModel;
import com.bitfire.uracer.game.world.models.TrackTrees;
import com.bitfire.uracer.game.world.models.TrackWalls;
import com.bitfire.uracer.game.world.models.TreeStillModel;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.NumberString;
import com.bitfire.uracer.utils.ScaleUtils;
import com.bitfire.uracer.utils.SpriteBatchUtils;
import com.bitfire.utils.ItemsManager;

public final class DebugHelper extends GameTask implements DisposableTasks {
	// render flags for basic debug info
	public enum RenderFlags {
		// @off
		VersionInfo,
		FpsStats,
		MemoryStats,
		MeshStats,
		PlayerInfo,
		PlayerCarInfo,
		PostProcessorInfo,
		PerformanceGraph,
		Box2DWireframe,
		BoundingBoxes3D,
		TrackSectors,
		Rankings
		// @on
	}

	// default render flags
	//@off
	private Set<RenderFlags> renderFlags = EnumSet.of(
		RenderFlags.VersionInfo,
		RenderFlags.FpsStats,
		RenderFlags.MeshStats,
		RenderFlags.PostProcessorInfo,
		RenderFlags.PerformanceGraph,
		RenderFlags.PlayerCarInfo,
		RenderFlags.Rankings
	);
	//@on

	private final ItemsManager<DebugRenderable> renderables = new ItemsManager<DebugRenderable>();

	private PostProcessor postProcessor;
	private final Matrix4 idt = new Matrix4();
	private Matrix4 xform;

	// frame stats
	private DebugStatistics stats;
	private String uRacerInfo;

	// world
	private GameWorld gameWorld;
	private World box2dWorld;

	// ranking
	private LapManager lapManager;

	// debug renderers
	private Box2DDebugRenderer b2drenderer;
	private ImmediateModeRenderer20 dbg = new ImmediateModeRenderer20(false, true, 0);

	public DebugHelper (GameWorld gameWorld, PostProcessor postProcessor, LapManager lapManager) {
		this.gameWorld = gameWorld;
		this.postProcessor = postProcessor;
		this.lapManager = lapManager;
		this.box2dWorld = gameWorld.getBox2DWorld();

		GameEvents.gameRenderer.addListener(renderListener, GameRendererEvent.Type.BatchDebug, GameRendererEvent.Order.PLUS_4);
		GameEvents.gameRenderer.addListener(renderListener, GameRendererEvent.Type.Debug, GameRendererEvent.Order.PLUS_4);

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

	public void add (DebugRenderable renderable) {
		renderables.add(renderable);
	}

	public void remove (DebugRenderable renderable) {
		renderables.remove(renderable);
	}

	public void toggleFlag (RenderFlags flag) {
		if (renderFlags.contains(flag)) {
			renderFlags.remove(flag);
		} else {
			renderFlags.add(flag);
		}
	}

	public void clearFlags () {
		renderFlags.clear();
	}

	public void setFlags (EnumSet<RenderFlags> set) {
		renderFlags = set;
	}

	public boolean isEnabled () {
		return !renderFlags.isEmpty();
	}

	@Override
	public void dispose () {
		super.dispose();

		GameEvents.gameRenderer.removeListener(renderListener, GameRendererEvent.Type.BatchDebug, GameRendererEvent.Order.PLUS_4);
		GameEvents.gameRenderer.removeListener(renderListener, GameRendererEvent.Type.Debug, GameRendererEvent.Order.PLUS_4);

		dbg.dispose();
		b2drenderer.dispose();
		stats.dispose();
		disposeTasks();
	}

	private final GameRendererEvent.Listener renderListener = new GameRendererEvent.Listener() {
		@Override
		public void handle (Object source, Type type, Order order) {
			if (renderFlags.isEmpty()) return;

			SpriteBatch batch = GameEvents.gameRenderer.batch;

			if (type == GameRendererEvent.Type.BatchDebug) {
				// render everything scaled
				for (DebugRenderable r : renderables) {
					if (renderFlags.contains(r.getFlag())) r.renderBatch(batch);
				}

				// save original transform matrix
				xform = batch.getTransformMatrix();
				batch.setTransformMatrix(idt);

				// render static debug information unscaled
				render(batch);

				// restore original matrix
				batch.setTransformMatrix(xform);

			} else if (type == GameRendererEvent.Type.Debug) {
				if (renderFlags.contains(RenderFlags.BoundingBoxes3D)) {
					renderBoundingBoxes(GameEvents.gameRenderer.camPersp);
				}

				if (renderFlags.contains(RenderFlags.Box2DWireframe)) {
					b2drenderer.render(box2dWorld, GameEvents.gameRenderer.mtxOrthographicMvpMt);
				}

				for (DebugRenderable r : renderables) {
					if (renderFlags.contains(r.getFlag())) r.render();
				}
			}
		}
	};

	@Override
	protected void onTick () {
		stats.update();
		for (DebugRenderable r : renderables) {
			r.tick();
		}
	}

	@Override
	public void onReset () {
		for (DebugRenderable r : renderables) {
			r.reset();
		}
	}

	@Override
	public void onPlayer (PlayerCar player) {
		super.onPlayer(player);

		for (DebugRenderable r : renderables) {
			r.player(player);
		}
	}

	@Override
	public void disposeTasks () {
		renderables.dispose();
	}

	private void render (SpriteBatch batch) {
		// batch.setTransformMatrix(idt);

		if (renderFlags.contains(RenderFlags.VersionInfo)) {
			renderVersionInfo(batch, Art.DebugFontHeight * 2);
		}

		if (renderFlags.contains(RenderFlags.FpsStats)) {
			renderFpsStats(batch, ScaleUtils.PlayHeight - Art.DebugFontHeight);
		}

		if (renderFlags.contains(RenderFlags.PerformanceGraph)) {
			renderGraphicalStats(batch, Art.DebugFontHeight * 2);
		}

		if (renderFlags.contains(RenderFlags.MemoryStats)) {
			renderMemoryUsage(batch, ScaleUtils.PlayHeight - Art.DebugFontHeight * 4);
		}

		if (renderFlags.contains(RenderFlags.PlayerInfo)) {
			renderPlayerInfo(batch, 0);
		}

		if (renderFlags.contains(RenderFlags.PostProcessorInfo)) {
			renderPostProcessorInfo(batch, ScaleUtils.PlayHeight - Art.DebugFontHeight);
		}

		if (renderFlags.contains(RenderFlags.Rankings)) {
			renderRankings(batch, renderFlags.contains(RenderFlags.PlayerInfo) ? Art.DebugFontHeight * 10 : 0);
		}

		if (renderFlags.contains(RenderFlags.MeshStats)) {
			SpriteBatchUtils.drawString(batch, "total meshes=" + GameWorld.TotalMeshes, 0, ScaleUtils.PlayHeight
				- Art.DebugFontHeight * 3);
			SpriteBatchUtils.drawString(batch, "rendered meshes="
				+ (GameWorldRenderer.renderedTrees + GameWorldRenderer.renderedWalls) + ", trees=" + GameWorldRenderer.renderedTrees
				+ ", walls=" + GameWorldRenderer.renderedWalls + ", culled=" + GameWorldRenderer.culledMeshes, 0,
				ScaleUtils.PlayHeight - Art.DebugFontHeight * 2);
		}
	}

	private void renderRankings (SpriteBatch batch, int y) {
		if (!hasPlayer) return;

		ReplayInfo last = lapManager.getLastRecording();
		boolean discarded = last != null && !last.accepted;

		int coord = y;

		SpriteBatchUtils.drawString(batch, "CURRENT RANKINGS", 0, coord);
		SpriteBatchUtils.drawString(batch, "================", 0, coord + Art.DebugFontHeight);

		coord += 2 * Art.DebugFontHeight;
		int rank = 1;
		for (Replay replay : lapManager.getReplays()) {
			boolean lastAccepted = false;

			if (last != null && last.accepted) {
				lastAccepted = last.replay.getReplayId().equals(replay.getReplayId());
			}

			if (lastAccepted) {
				batch.end();
				batch.flush();
				if (last.position == 1)
					batch.setColor(0, 1, 0, 1);
				else
					batch.setColor(1, 1, 0, 1);
				batch.begin();
			}

			SpriteBatchUtils.drawString(batch,
				"#" + (rank <= 9 ? "0" : "") + rank + " " + replay.getUserId() + " " + replay.getTrackTimeInt() / 1000f, 0, coord);

			if (lastAccepted) {
				batch.end();
				batch.flush();
				batch.setColor(1, 1, 1, 1);
				batch.begin();
			}

			coord += Art.DebugFontHeight;
			rank++;
		}

		// show discarded lap
		if (discarded) {
			Replay replay = last.removed;

			batch.end();
			batch.flush();
			batch.setColor(1, 0, 0, 1);
			batch.begin();

			SpriteBatchUtils.drawString(
				batch,
				"#" + (rank <= 9 ? "0" : "") + (lapManager.getReplays().size + 1) + " " + replay.getUserId() + " "
					+ replay.getTrackTimeInt() / 1000f, 0, coord);

			batch.end();
			batch.flush();
			batch.setColor(1, 1, 1, 1);
			batch.begin();
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
		String fps = NumberString.formatLong(Gdx.graphics.getFramesPerSecond());
		String phy = NumberString.formatLong(stats.meanPhysics.getMean());
		String gfx = NumberString.formatLong(stats.meanRender.getMean());
		String ticks = NumberString.formatLong(stats.meanTickCount.getMean());
		String text = "fps: " + fps + ", phy: " + phy + ", gfx: " + gfx + ", ticks: " + ticks;
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

	private void renderPostProcessorInfo (SpriteBatch batch, int y) {
		String text = "";

		if (postProcessor == null) {
			text = "No post-processor is active";
		} else {
			text = "Post-processing fx count = " + postProcessor.getEnabledEffectsCount();
		}

		SpriteBatchUtils.drawString(batch, text, 0, y);
	}

	private void renderPlayerInfo (SpriteBatch batch, int y) {
		if (!hasPlayer) return;

		CarDescriptor carDesc = player.getCarDescriptor();
		Body body = player.getBody();
		Vector2 pos = GameRenderer.ScreenUtils.worldMtToScreen(body.getPosition());
		EntityRenderState state = player.state();

		SpriteBatchUtils.drawString(batch, "vel_wc len =" + carDesc.velocity_wc.len(), 0, y);
		SpriteBatchUtils.drawString(batch, "vel_wc [x=" + carDesc.velocity_wc.x + ", y=" + carDesc.velocity_wc.y + "]", 0, y
			+ Art.DebugFontWidth);
		SpriteBatchUtils.drawString(batch, "steerangle=" + carDesc.steerangle, 0, y + Art.DebugFontWidth * 2);
		SpriteBatchUtils.drawString(batch, "throttle=" + carDesc.throttle, 0, y + Art.DebugFontWidth * 3);
		SpriteBatchUtils.drawString(batch, "screen x=" + pos.x + ",y=" + pos.y, 0, y + Art.DebugFontWidth * 4);
		SpriteBatchUtils.drawString(batch, "world-mt x=" + body.getPosition().x + ",y=" + body.getPosition().y, 0, y
			+ Art.DebugFontWidth * 5);
		SpriteBatchUtils.drawString(batch,
			"world-px x=" + Convert.mt2px(body.getPosition().x) + ",y=" + Convert.mt2px(body.getPosition().y), 0, y
				+ Art.DebugFontWidth * 6);
		SpriteBatchUtils.drawString(batch, "orient=" + body.getAngle(), 0, y + Art.DebugFontWidth * 7);
		SpriteBatchUtils.drawString(batch, "render.interp=" + (state.position.x + "," + state.position.y), 0, y
			+ Art.DebugFontWidth * 8);

		// BatchUtils.drawString( batch, "on tile " + tilePosition, 0, 0 );
	}

	private void renderBoundingBoxes (PerspectiveCamera camPersp) {
		// trees
		TrackTrees trees = gameWorld.getTrackTrees();
		TrackWalls walls = gameWorld.getTrackWalls();

		for (int i = 0; i < trees.models.size(); i++) {
			TreeStillModel m = trees.models.get(i);
			renderBoundingBox(camPersp, m.boundingBox);
		}

		for (int i = 0; i < walls.count(); i++) {
			OrthographicAlignedStillModel m = walls.models.get(i);
			renderBoundingBox(camPersp, m.boundingBox);
		}
	}

	/** This is intentionally SLOW. Read it again!
	 * 
	 * @param boundingBox */
	private void renderBoundingBox (PerspectiveCamera camPersp, BoundingBox boundingBox) {
		float alpha = .15f;
		float r = 0f;
		float g = 0f;
		float b = 1f;
		float offset = 0.5f; // offset for the base, due to pixel-perfect model placement

		Vector3[] corners = boundingBox.getCorners();

		Gdx.gl.glDisable(GL20.GL_CULL_FACE);
		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

		dbg.begin(camPersp.combined, GL10.GL_TRIANGLES);
		{
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[0].x, corners[0].y, corners[0].z + offset);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[1].x, corners[1].y, corners[1].z + offset);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[4].x, corners[4].y, corners[4].z);

			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[1].x, corners[1].y, corners[1].z + offset);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[4].x, corners[4].y, corners[4].z);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[5].x, corners[5].y, corners[5].z);

			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[1].x, corners[1].y, corners[1].z + offset);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[2].x, corners[2].y, corners[2].z + offset);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[5].x, corners[5].y, corners[5].z);

			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[2].x, corners[2].y, corners[2].z + offset);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[5].x, corners[5].y, corners[5].z);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[6].x, corners[6].y, corners[6].z);

			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[2].x, corners[2].y, corners[2].z + offset);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[6].x, corners[6].y, corners[6].z);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[3].x, corners[3].y, corners[3].z + offset);

			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[3].x, corners[3].y, corners[3].z);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[6].x, corners[6].y, corners[6].z);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[7].x, corners[7].y, corners[7].z);

			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[3].x, corners[3].y, corners[3].z + offset);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[0].x, corners[0].y, corners[0].z + offset);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[7].x, corners[7].y, corners[7].z);

			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[7].x, corners[7].y, corners[7].z);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[0].x, corners[0].y, corners[0].z + offset);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[4].x, corners[4].y, corners[4].z);

			// top cap
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[4].x, corners[4].y, corners[4].z);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[5].x, corners[5].y, corners[5].z);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[7].x, corners[7].y, corners[7].z);

			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[5].x, corners[5].y, corners[5].z);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[7].x, corners[7].y, corners[7].z);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[6].x, corners[6].y, corners[6].z);

			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[0].x, corners[0].y, corners[0].z + offset);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[3].x, corners[3].y, corners[3].z + offset);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[1].x, corners[1].y, corners[1].z + offset);

			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[3].x, corners[3].y, corners[3].z + offset);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[1].x, corners[1].y, corners[1].z + offset);
			dbg.color(r, g, b, alpha);
			dbg.vertex(corners[2].x, corners[2].y, corners[2].z + offset);
		}
		dbg.end();

		Gdx.gl.glDisable(GL20.GL_BLEND);
	}
}
