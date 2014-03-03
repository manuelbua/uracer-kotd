
package com.bitfire.uracer.game.debug;

import java.util.EnumSet;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
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
import com.badlogic.gdx.utils.Array;
import com.bitfire.postprocessing.PostProcessor;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.entities.EntityRenderState;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.GameLogic;
import com.bitfire.uracer.game.actors.CarDescriptor;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.events.GameRendererEvent;
import com.bitfire.uracer.game.events.GameRendererEvent.Order;
import com.bitfire.uracer.game.events.GameRendererEvent.Type;
import com.bitfire.uracer.game.logic.gametasks.DisposableTasks;
import com.bitfire.uracer.game.logic.gametasks.GameTask;
import com.bitfire.uracer.game.logic.helpers.GameTrack.TrackState;
import com.bitfire.uracer.game.logic.replaying.LapManager;
import com.bitfire.uracer.game.logic.replaying.Replay;
import com.bitfire.uracer.game.logic.replaying.ReplayInfo;
import com.bitfire.uracer.game.logic.replaying.ReplayManager;
import com.bitfire.uracer.game.logic.replaying.ReplayManager.ReplayResult;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.rendering.GameWorldRenderer;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.game.world.models.OrthographicAlignedStillModel;
import com.bitfire.uracer.game.world.models.TrackTrees;
import com.bitfire.uracer.game.world.models.TrackWalls;
import com.bitfire.uracer.game.world.models.TreeStillModel;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.ReplayUtils;
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
		MusicVolumes,
		CarEngineVolumes,
		Rankings,
		Completion
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
		RenderFlags.MusicVolumes,
		RenderFlags.CarEngineVolumes,
		RenderFlags.Rankings,
		RenderFlags.Completion
	);
	//@on

	private final ItemsManager<DebugRenderable> renderables = new ItemsManager<DebugRenderable>();

	private PostProcessor postProcessor;
	private final Matrix4 idt = new Matrix4();
	private Matrix4 xform;
	private Input input;
	private boolean enabled;

	// frame stats
	private DebugStatistics stats;
	private String uRacerInfo;

	// world
	private GameWorld gameWorld;
	private World box2dWorld;
	private GameLogic logic;

	// ranking
	private LapManager lapManager;
	private Array<RankInfo> ranks = new Array<RankInfo>();

	// debug renderers
	private Box2DDebugRenderer b2drenderer;
	private ImmediateModeRenderer20 dbg = new ImmediateModeRenderer20(false, true, 0);

	public DebugHelper (GameWorld gameWorld, PostProcessor postProcessor, LapManager lapManager, GameLogic logic, Input input) {
		this.gameWorld = gameWorld;
		this.postProcessor = postProcessor;
		this.lapManager = lapManager;
		this.logic = logic;
		this.input = input;

		this.enabled = Config.Debug.UseDebugHelper;
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

		for (int i = 0; i < ReplayManager.MaxReplays + 1; i++) {
			ranks.add(new RankInfo());
		}
	}

	/** Adds a DebugRenderable */
	public void add (DebugRenderable renderable) {
		renderables.add(renderable);
	}

	/** Removes a DebugRenderable */
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
		return enabled && !renderFlags.isEmpty();
	}

	public void setEnabled (boolean enabled) {
		this.enabled = enabled;
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
			if (!isEnabled()) return;

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

	private void handleInput () {
		if (input.isPressed(Keys.W)) {
			toggleFlag(RenderFlags.Box2DWireframe);
		} else if (input.isPressed(Keys.B)) {
			toggleFlag(RenderFlags.BoundingBoxes3D);
		} else if (input.isPressed(Keys.S)) {
			toggleFlag(RenderFlags.TrackSectors);
		}
	}

	@Override
	protected void onTick () {
		if (isEnabled()) {
			handleInput();
		}
	}

	@Override
	protected void onTickCompleted () {
		stats.update();
		for (DebugRenderable r : renderables) {
			r.tick();
		}
	}

	@Override
	public void onGameReset () {
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

		if (renderFlags.contains(RenderFlags.Completion)) {
			renderCompletion(batch, renderFlags.contains(RenderFlags.PlayerInfo) ? Art.DebugFontHeight * 10 : 0);
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

	private void batchColorStart (SpriteBatch batch, float r, float g, float b) {
		batch.end();
		batch.flush();
		batch.setColor(r, g, b, 1);
		batch.begin();
	}

	private void batchColorEnd (SpriteBatch batch) {
		batch.end();
		batch.flush();
		batch.setColor(1, 1, 1, 1);
		batch.begin();
	}

	private String timeString (float seconds) {
		return String.format("%02.03f", seconds);
	}

	private String rankString (int rank) {
		return (rank <= 9 ? "0" : "") + rank;
	}

	private boolean isNextTarget (Replay replay) {
		GhostCar target = logic.getNextTarget();
		if (target != null) {
			if (replay != null && target.getReplay().getId().equals(replay.getId())) {
				return true;
			}
		}

		return false;
	}

	private void drawString2X (SpriteBatch batch, String text, float x, float y, float scale) {
		SpriteBatchUtils.drawString(batch, text, x, y, Art.DebugFontWidth * scale, Art.DebugFontHeight * scale);
	}

	private void renderRankings (SpriteBatch batch, int y) {
		// if (!hasPlayer) return;

		float scale = 1;
		ReplayResult last = logic.getLastRecordedInfo();
		boolean discarded = last != null && !last.is_accepted;

		float xoffset = 5;
		int coord = y;
		drawString2X(batch, "CURRENT RANKINGS", xoffset, coord, scale);
		drawString2X(batch, "================", xoffset, coord + Art.DebugFontHeight * scale, scale);
		coord += 2 * Art.DebugFontHeight * scale;

		String text;
		int rank = 1;
		for (Replay replay : lapManager.getReplays()) {
			boolean lastAccepted = false;

			if (last != null && last.is_accepted) {
				lastAccepted = last.accepted.getId().equals(replay.getId());
			}

			if (lastAccepted) {
				if (last.position == 1)
					batchColorStart(batch, 0, 1, 0);
				else
					batchColorStart(batch, 1, 1, 0);
			} else {
				if (isNextTarget(replay)) {
					batchColorStart(batch, 0.5f, 0.9f, 1);
				}
			}

			text = rankString(rank) + (isNextTarget(replay) ? "<" : " ") + " " + replay.getUserId() + " " + replay.getSecondsStr();
			drawString2X(batch, text, xoffset, coord, scale);

			batchColorEnd(batch);

			coord += Art.DebugFontHeight * scale;
			rank++;
		}

		// show discarded lap
		if (discarded && last != null && last.discarded.isValid()) {
			ReplayInfo info = last.discarded;
			batchColorStart(batch, 1, 0, 0);

			text = rankString(lapManager.getReplays().size + 1) + "  " + info.getUserId() + " " + info.getSecondsStr();
			drawString2X(batch, text, xoffset, coord, scale);
			batchColorEnd(batch);
		}
	}

	private void renderCompletion (SpriteBatch batch, int y) {

		//
		// update
		//

		float scale = 1;
		float xoffset = 130 * scale;
		int coord = y;
		for (int i = 0; i < ReplayManager.MaxReplays + 1; i++) {
			ranks.get(i).reset();
		}

		int playerIndex = 0;
		for (int i = 0; i < ReplayManager.MaxReplays; i++) {
			GhostCar ghost = logic.getGhost(i);
			TrackState ts = ghost.getTrackState();
			if (ghost != null && ghost.hasReplay()) {
				RankInfo rank = ranks.get(i);
				rank.valid = true;
				rank.uid = ghost.getReplay().getUserId();
				rank.ticks = ghost.getReplay().getTicks();
				rank.isNextTarget = isNextTarget(ghost.getReplay());
				rank.player = false;

				rank.completion = 0;
				if (ts.ghostArrived) {
					rank.completion = 1;
				} else if (ghost.isPlaying()) {
					rank.completion = gameWorld.getGameTrack().getTrackCompletion(ghost);
				}

				// Gdx.app.log("", "arrived=" + ts.ghostArrived);
				// rank.completion = c;
				playerIndex++;
			}
		}

		if (hasPlayer) {
			RankInfo rank = ranks.get(playerIndex);
			rank.valid = true;
			rank.uid = logic.getUserProfile().userId;
			rank.player = true;
			rank.isNextTarget = false;

			// getTrackTimeInt is computed as an int cast (int)(trackTimeSeconds * AMath.ONE_ON_CMP_EPSILON)
			rank.ticks = lapManager.getCurrentReplayTicks();
			rank.completion = logic.isWarmUp() ? 0 : gameWorld.getGameTrack().getTrackCompletion(player);
		}

		ranks.sort();

		//
		// render
		//

		drawString2X(batch, "CURRENT COMPLETION", xoffset, coord, scale);
		drawString2X(batch, "==================", xoffset, coord + Art.DebugFontHeight * scale, scale);
		coord += 2 * Art.DebugFontHeight * scale;

		String text;
		for (int i = 0; i < ranks.size; i++) {
			RankInfo r = ranks.get(i);
			if (r.valid) {
				float c = AMath.fixupTo(AMath.fixup(r.completion), 1);
				if (r.player) {
					batchColorStart(batch, 0, 0.6f, 1);
				} else {
					if (r.isNextTarget) {
						batchColorStart(batch, 0.5f, 0.9f, 1);
					}
				}

				text = rankString(i + 1) + (r.isNextTarget ? "<" : " ") + " " + r.uid + " " + timeString(c) + " "
					+ String.format("%.03f", ReplayUtils.ticksToSeconds(r.ticks));
				drawString2X(batch, text, xoffset, coord, scale);

				batchColorEnd(batch);

				coord += Art.DebugFontHeight * scale;
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
		String fps = String.format("%d", Gdx.graphics.getFramesPerSecond());
		String phy = String.format("%.04f", stats.meanPhysics.getMean());
		String gfx = String.format("%.04f", stats.meanRender.getMean());
		String ticks = String.format("%.02f", stats.meanTickCount.getMean());
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

		String text = "java heap = " + String.format("%.02f", javaHeapMb) + "MB" + " - native heap = "
			+ String.format("%.02f", nativeHeapMb) + "MB";

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

		dbg.begin(camPersp.combined, GL20.GL_TRIANGLES);
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
