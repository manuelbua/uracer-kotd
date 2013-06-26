
package com.bitfire.uracer.game.logic;

import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.equations.Linear;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.postprocessing.PostProcessor;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.game.GameplaySettings;
import com.bitfire.uracer.game.Time;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.debug.DebugHelper;
import com.bitfire.uracer.game.debug.DebugHelper.RenderFlags;
import com.bitfire.uracer.game.debug.GameTrackDebugRenderer;
import com.bitfire.uracer.game.debug.player.DebugPlayer;
import com.bitfire.uracer.game.events.CarEvent;
import com.bitfire.uracer.game.logic.replaying.ReplayRecorder.RecorderError;
import com.bitfire.uracer.game.logic.types.CommonLogic;
import com.bitfire.uracer.game.logic.types.helpers.TimeModulator;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.rendering.GameWorldRenderer;
import com.bitfire.uracer.game.tween.GameTweener;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.BoxedFloat;
import com.bitfire.uracer.utils.BoxedFloatAccessor;
import com.bitfire.uracer.utils.InterpolatedFloat;

public abstract class BaseLogic extends CommonLogic {
	private DebugHelper debug = null;
	private Vector2 cameraPos = new Vector2();
	private float prevZoom = GameWorldRenderer.MinCameraZoom + GameWorldRenderer.ZoomWindow;
	private InterpolatedFloat driftStrength = new InterpolatedFloat();
	private TimeModulator timeMod = null;
	private Time dilationTime;
	private Time outOfTrackTime;

	public BaseLogic (UserProfile userProfile, GameWorld gameWorld, GameRenderer gameRenderer) {
		super(userProfile, gameWorld, gameRenderer);
		timeMod = new TimeModulator();
		dilationTime = new Time();
		outOfTrackTime = new Time();
		setupDebug(gameRenderer.getPostProcessing().getPostProcessor());
	}

	@Override
	public void dispose () {
		destroyDebug();
		super.dispose();
	}

	private void setupDebug (PostProcessor postProcessor) {
		if (Config.Debug.UseDebugHelper) {
			debug = new DebugHelper(gameWorld, postProcessor);
			debug.add(new GameTrackDebugRenderer(RenderFlags.TrackSectors, gameWorld.getGameTrack()));
			debug.add(new DebugPlayer(RenderFlags.PlayerCarInfo, gameTasksManager));
			Gdx.app.debug("Game", "Debug helper initialized");
		}
	}

	private void destroyDebug () {
		if (Config.Debug.UseDebugHelper) {
			debug.dispose();
		}
	}

	@Override
	public Time getOutOfTrackTimer () {
		return outOfTrackTime;
	}

	@Override
	public Time getTimeDilationTimer () {
		return dilationTime;
	}

	@Override
	public TimeModulator getTimeModulator () {
		return timeMod;
	}

	@Override
	public void handleExtraInput () {
		if (inputSystem.isPressed(Keys.O)) {
			removePlayer();
			restartGame();
			restartAllReplays();
		} else if (inputSystem.isPressed(Keys.P)) {
			addPlayer();
			restartGame();
		} else if (inputSystem.isPressed(Keys.TAB)) {
			gameRenderer.setDebug(!gameRenderer.isDebugEnabled());
		}

		if (gameRenderer.isDebugEnabled() && debug.isEnabled()) {
			if (inputSystem.isPressed(Keys.W)) {
				debug.toggleFlag(RenderFlags.Box2DWireframe);
			} else if (inputSystem.isPressed(Keys.B)) {
				debug.toggleFlag(RenderFlags.BoundingBoxes3D);
			} else if (inputSystem.isPressed(Keys.S)) {
				debug.toggleFlag(RenderFlags.TrackSectors);
			}
		}
	}

	@Override
	public void startTimeDilation () {
		dilationTime.start();
		timeMod.toDilatedTime();
	}

	@Override
	public void endTimeDilation () {
		// reset it, endTimeDilation can be called out of GameInput as well
		gameInput.resetTimeDilating();
		dilationTime.reset();
		timeMod.toNormalTime();
	}

	@Override
	public void removePlayer () {
		super.removePlayer();
		driftStrength.reset(0, true);
	}

	@Override
	public float getCollisionFactor () {
		return collisionFactor.value;
	}

	@Override
	public void beforeRender () {
		// request camera updates from callbacks
		float zoom = updateCameraZoom(URacer.Game.getTimeModFactor());
		updateCameraPosition(cameraPos);

		// apply camera updates
		gameWorldRenderer.setCameraZoom(zoom);
		gameWorldRenderer.setCameraPosition(cameraPos);
		gameWorldRenderer.updateCamera();

		// sync post-processing animators
		postProcessing.onBeforeRender(zoom, playerLapMonitor.getWarmUpCompletion(), collisionFactor.value);

		// game tweener step
		GameTweener.update();
	}

	@Override
	public float updateCameraZoom (float timeModFactor) {
		if (hasPlayer()) {
			// speed.set(playerCar.carState.currSpeedFactor, 0.02f);
			driftStrength.set(playerCar.driftState.driftStrength, 0.02f);
		}

		float minZoom = GameWorldRenderer.MinCameraZoom;
		float maxZoom = GameWorldRenderer.MaxCameraZoom;

		float cameraZoom = (minZoom + GameWorldRenderer.ZoomWindow);
		cameraZoom += (maxZoom - cameraZoom) * timeModFactor;
		cameraZoom += 0.25f * GameWorldRenderer.ZoomWindow * driftStrength.get();

		cameraZoom = AMath.lerp(prevZoom, cameraZoom, 0.1f);
		cameraZoom = AMath.clampf(cameraZoom, minZoom, maxZoom);
		cameraZoom = AMath.fixupTo(cameraZoom, minZoom + GameWorldRenderer.ZoomWindow);

		prevZoom = cameraZoom;
		return cameraZoom;
	}

	private final BoxedFloat collisionFactor = new BoxedFloat(0);
	private float lastImpactForce = 0;

	private TweenCallback collisionFinished = new TweenCallback() {
		@Override
		public void onEvent (int type, BaseTween<?> source) {
			switch (type) {
			case COMPLETE:
				lastImpactForce = 0;
			}
		}
	};

	@Override
	public void collision (CarEvent.Data data) {
		if (gameInput.isTimeDilating()) {
			endTimeDilation();
		}

		float clampedImpactForce = AMath.normalizeImpactForce(data.impulses.len());

		// while busy, a new collision factor will be accepted *only* if stronger
		if (clampedImpactForce > 0 && clampedImpactForce > lastImpactForce) {
			lastImpactForce = clampedImpactForce;

			GameTweener.stop(collisionFactor);
			collisionFactor.value = 0;

			final float min = GameplaySettings.CollisionFactorMinDurationMs;
			final float max = GameplaySettings.CollisionFactorMaxDurationMs;

			//@off
			GameTweener.start(Timeline
				.createSequence()
				.push(Tween.to(collisionFactor, BoxedFloatAccessor.VALUE,100).target(clampedImpactForce).ease(Linear.INOUT))
				.push(Tween.to(collisionFactor, BoxedFloatAccessor.VALUE,min + max * clampedImpactForce).target(0)
					.ease(Linear.INOUT)).setCallback(collisionFinished));
			//@on

			playerTasks.hudPlayer.highlightCollision();
		}
	}

	@Override
	public void physicsForcesReady (CarEvent.Data eventData) {
		RecorderError recerror = lapManager.record(eventData.forces);
		if (recerror == RecorderError.ReplayMemoryLimitReached) {
			Gdx.app.log("CommonLogic", "Player too slow, recording aborted.");
			playerError("Too slow!");
		}
	}

	@Override
	public void ghostFadingOut (GhostCar ghost) {
		if (ghost != null && ghost == getNextTarget()) {
			playerTasks.hudPlayer.unHighlightNextTarget();
		}
	}

	@Override
	public void ghostLapStarted (GhostCar ghost) {
	}

	@Override
	public void driftBegins (PlayerCar player) {
		playerTasks.hudPlayer.beginDrift();
	}

	@Override
	public void driftEnds (PlayerCar player) {
		playerTasks.hudPlayer.endDrift();
	}

	@Override
	public void wrongWayBegins () {
		playerTasks.hudPlayer.wrongWay.fadeIn();
		playerError("Invalid lap");
	}

	@Override
	public void wrongWayEnds () {
	}

	@Override
	public void outOfTrack () {
		outOfTrackTime.start();
	}

	@Override
	public void backInTrack () {
		outOfTrackTime.reset();
	}

	//
	// utilities
	//

	/** Invalidates the current lap and shows an error */
	protected void playerError (String message) {
		isCurrentLapValid = false;
		lapManager.abortRecording(true);
		playerTasks.hudLapInfo.setInvalid(message);
		playerTasks.hudLapInfo.toColor(1, 0, 0);
		postProcessing.alertBegins(500);
	}

}
