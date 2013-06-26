
package com.bitfire.uracer.game.logic;

import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.equations.Linear;

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

public abstract class BaseLogic extends CommonLogic {
	private Vector2 cameraPos = new Vector2();
	private float prevZoom = GameWorldRenderer.MinCameraZoom + GameWorldRenderer.ZoomWindow;
	private InterpolatedFloat driftStrength = new InterpolatedFloat();
	private TimeModulator timeMod = null;
	private Time dilationTime = new Time();
	private Time outOfTrackTime = new Time();

	public BaseLogic (UserProfile userProfile, GameWorld gameWorld, GameRenderer gameRenderer) {
		super(userProfile, gameWorld, gameRenderer);
		timeMod = new TimeModulator();
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
	public void ghostLapStarted (GhostCar ghost) {
	}

	@Override
	public void backInTrack () {
		outOfTrackTime.reset();
	}
}
