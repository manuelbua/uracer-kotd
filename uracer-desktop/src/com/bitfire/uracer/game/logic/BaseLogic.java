
package com.bitfire.uracer.game.logic;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.game.GameplaySettings;
import com.bitfire.uracer.game.Time;
import com.bitfire.uracer.game.actors.GhostCar;
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

import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.equations.Linear;

public abstract class BaseLogic extends CommonLogic {
	private Vector2 cameraPos = new Vector2();
	private static final float ZoomNorm = 0.35f;

	private float prevZoom = GameWorldRenderer.MaxCameraZoom - ZoomNorm;
	private InterpolatedFloat driftStrength = new InterpolatedFloat();
	private InterpolatedFloat speed = new InterpolatedFloat();
	private TimeModulator timeMod = null;
	private Time dilationTime;
	private Time outOfTrackTime;
	private final BoxedFloat collisionFactor = new BoxedFloat(0);
	private float collisionFrontRatio = 0.5f;
	private float lastImpactForce = 0;

	public BaseLogic (UserProfile userProfile, GameWorld gameWorld, GameRenderer gameRenderer) {
		super(userProfile, gameWorld, gameRenderer);
		timeMod = new TimeModulator();
		dilationTime = new Time();
		outOfTrackTime = new Time();
	}

	@Override
	public void dispose () {
		super.dispose();
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
	public void startTimeDilation () {
		dilationTime.start();
		timeMod.toDilatedTime();
	}

	@Override
	public void endTimeDilation () {
		gameInput.resetTimeDilating(); // reset it since endTimeDilation can be called out of GameInput as well
		dilationTime.reset();
		timeMod.toNormalTime();
	}

	@Override
	public void removePlayer () {
		super.removePlayer();
		driftStrength.reset(0, true);
		speed.reset(0, true);
	}

	@Override
	public float getCollisionFactor () {
		return collisionFactor.value;
	}

	@Override
	public float getCollisionFrontRatio () {
		return collisionFrontRatio;
	}

	@Override
	public void endCollisionTime () {
		GameTweener.stop(collisionFactor);
		collisionFrontRatio = 0.5f;
		lastImpactForce = 0;

		if (!AMath.isZero(collisionFactor.value)) {
			//@off
			GameTweener.start(Timeline
				.createSequence()
				.push(Tween.to(collisionFactor, BoxedFloatAccessor.VALUE, 500).target(0).ease(Linear.INOUT)));
			//@on
		}
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

		// update lights
		// sync post-processing animators
		postProcessing.onBeforeRender(gameWorldRenderer.getCameraPosition(), progressData, gameWorldRenderer.getAmbientColor(),
			gameWorldRenderer.getTreesAmbientColor(), zoom, playerLapMonitor.getWarmUpCompletion(), collisionFactor.value, paused);

		gameWorldRenderer.updateRayHandler();

		// game tweener step
		if (!paused) GameTweener.update();
	}

	@Override
	public float updateCameraZoom (float timeModFactor) {
		if (hasPlayer()) {
			speed.set(playerCar.carState.currSpeedFactor, 0.02f);
			driftStrength.set(playerCar.driftState.driftStrength, 0.02f);
		}

		float minZoom = GameWorldRenderer.MinCameraZoom;
		float maxZoom = GameWorldRenderer.MaxCameraZoom;

		// dbg
		// ZoomNorm = 0.2f;
		// collisionFactor.value = 0f;

		float cameraZoom = (minZoom + GameWorldRenderer.ZoomWindow);
		cameraZoom = maxZoom - ZoomNorm - collisionFactor.value * 0.1f;// (1 - ZoomNorm);
		// cameraZoom += 0.2f * timeModFactor; // zoom in if slowing time down
		cameraZoom = AMath.lerp(cameraZoom, minZoom - 0.1f, speed.get()); // pretend minZoom to be less than it is

		// TODO make it a toggleable option
		cameraZoom = AMath.lerp(cameraZoom, maxZoom, timeModFactor);
		cameraZoom = AMath.clamp(cameraZoom, minZoom, maxZoom);
		cameraZoom = AMath.lerp(cameraZoom, maxZoom, collisionFactor.value * 5f);

		cameraZoom = AMath.lerp(prevZoom, cameraZoom, 0.025f);
		cameraZoom = AMath.clamp(cameraZoom, minZoom, maxZoom * 2f); // relax max a bit

		// cameraZoom = 1;
		// Gdx.app.log("BaseLogic", "cameraZoom=" + cameraZoom + " [" + minZoom + ", " + maxZoom + "]");
		// Gdx.app.log("BaseLogic", "" + collisionFactor.value);

		// cameraZoom += GameWorldRenderer.ZoomRange * timeModFactor;
		// cameraZoom += 0.25f * GameWorldRenderer.ZoomWindow * driftStrength.get();
		// cameraZoom += (maxZoom - cameraZoom) * timeModFactor; // zoom in if slowing time down
		// cameraZoom -= (maxZoom - cameraZoom) * speed.get(); // zoom out if speedy
		// cameraZoom = AMath.lerp(prevZoom, cameraZoom, 0.1f);
		// cameraZoom = AMath.clampf(cameraZoom, minZoom, maxZoom);
		// cameraZoom = AMath.fixupTo(cameraZoom, minZoom + GameWorldRenderer.ZoomWindow);

		prevZoom = cameraZoom;
		return cameraZoom;
	}

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
		// stops time dilation
		// if (gameInput.isTimeDilating()) {
		// endTimeDilation();
		// }

		float clampedImpactForce = AMath.normalizeImpactForce(data.impulses.len());

		// while busy, a new collision factor will be accepted *only* if stronger than the previous one
		if (clampedImpactForce > 0 && clampedImpactForce > lastImpactForce) {
			lastImpactForce = clampedImpactForce;

			GameTweener.stop(collisionFactor);
			collisionFrontRatio = data.frontRatio;
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
		if (lapManager.isRecording()) {
			RecorderError recerror = lapManager.record(eventData.forces);

			switch (recerror) {
			case ReplayMemoryLimitReached:
				Gdx.app.log("CommonLogic", "Player too slow, recording aborted.");
				playerError("Too slow!");
				break;
			case RecordingNotEnabled:
				Gdx.app.log("CommonLogic", "Recording not enabled");
				playerError("Recording not enabled");
				break;
			case NoError:
				break;
			}
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
