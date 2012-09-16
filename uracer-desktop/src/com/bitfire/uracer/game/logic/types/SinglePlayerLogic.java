
package com.bitfire.uracer.game.logic.types;

import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.equations.Quad;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.game.Time;
import com.bitfire.uracer.game.Time.Reference;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.player.DriftBar;
import com.bitfire.uracer.game.logic.gametasks.messager.Messager;
import com.bitfire.uracer.game.logic.replaying.Replay;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.rendering.GameWorldRenderer;
import com.bitfire.uracer.game.tween.GameTweener;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.BoxedFloat;
import com.bitfire.uracer.utils.BoxedFloatAccessor;
import com.bitfire.uracer.utils.CarUtils;

public class SinglePlayerLogic extends CommonLogic {

	private Messager messager;
	private DriftBar driftBar;
	private BoxedFloat accuDriftSeconds;

	private Time dilationTime = new Time();
	private Time outOfTrackTime = new Time();
	private Timeline driftSecondsTimeline;
	private boolean isPenalty;

	private float lastCompletion, lastDist;

	public SinglePlayerLogic (UserProfile userProfile, GameWorld gameWorld, GameRenderer gameRenderer,
		ScalingStrategy scalingStrategy) {
		super(userProfile, gameWorld, gameRenderer, scalingStrategy);
		messager = gameTasksManager.messager;

		accuDriftSeconds = new BoxedFloat(0);
		dilationTime.stop();
		outOfTrackTime.stop();
	}

	@Override
	public void setPlayer (com.bitfire.uracer.game.actors.CarPreset.Type presetType) {
		super.setPlayer(presetType);
		driftBar = playerTasks.hudPlayer.driftBar;
	}

	@Override
	public void dispose () {
		super.dispose();
	}

	//
	// event listeners / callbacks
	//

	// the camera needs to be positioned
	@Override
	protected void updateCamera (float timeModFactor) {
		gameWorldRenderer.setCameraZoom(1.0f + (GameWorldRenderer.MaxCameraZoom - 1) * timeModFactor);

		// update player's headlights and move the world camera to follows it, if there is a player
		if (hasPlayer()) {

			if (gameWorld.isNightMode()) {
				gameWorldRenderer.updatePlayerHeadlights(playerCar);
			}

			gameWorldRenderer.setCameraPosition(playerCar.state().position, playerCar.state().orientation,
				playerCar.carState.currSpeedFactor);

		} else if (getGhost(0).hasReplay()) {

			gameWorldRenderer.setCameraPosition(getGhost(0).state().position, getGhost(0).state().orientation, 0);

		} else {

			// no ghost, no player, WTF?
			gameWorldRenderer.setCameraPosition(gameWorld.playerStartPos, gameWorld.playerStartOrient, 0);
		}
	}

	// the game has been restarted
	@Override
	protected void restart () {
		Gdx.app.log("SinglePlayerLogic", "Starting/restarting game");

		// restart all replays
		restartAllReplays();
		accuDriftSeconds.value = 0;
		isPenalty = false;
	}

	// the game has been reset
	@Override
	protected void reset () {
		Gdx.app.log("SinglePlayerLogic", "Resetting game");
		replayManager.reset();
		accuDriftSeconds.value = 0;
		isPenalty = false;
	}

	// a new Replay from the player is available: note that CommonLogic already perform
	// some basic filtering such as null checking, length validity, better-than-worst...
	@Override
	public void newReplay (Replay replay) {

		CarUtils.dumpSpeedInfo("Player", playerCar, replay.trackTimeSeconds);

		if (!replayManager.canClassify()) {
			getGhost(0).setReplay(replay);
			// messager.show("GO!  GO!  GO!", 3f, Type.Information, Position.Bottom, Size.Big);
		} else {
			Replay best = replayManager.getBestReplay();
			Replay worst = replayManager.getWorstReplay();

			float bestTime = AMath.round(best.trackTimeSeconds, 2);
			float worstTime = AMath.round(worst.trackTimeSeconds, 2);
			float diffTime = AMath.round(worstTime - bestTime, 2);

			if (AMath.equals(worstTime, bestTime)) {
				// draw!
// messager.show("DRAW!", 3f, Type.Information, Position.Bottom, Size.Big);
			} else {
				// has the player managed to beat the best lap?
// if (lapManager.isLastBestLap()) {
// messager.show("-" + NumberString.format(diffTime) + " seconds!", 3f, Type.Good, Position.Bottom, Size.Big);
// } else {
// messager.show("+" + NumberString.format(diffTime) + " seconds", 3f, Type.Bad, Position.Bottom, Size.Big);
// }
			}
		}
	}

	@Override
	public void tick () {
		super.tick();

		updateDriftBar();

		if (accuDriftSeconds.value == 0 && timeDilation) {
			requestTimeDilationFinish();
			Gdx.app.log("", "Requesting time modulation to finish");
		}

		if (hasPlayer() && playerCar.isOutOfTrack()) {
			playerTasks.hudPlayer.highlightOutOfTrack();
		}

// Gdx.app.log("SPL", "drift=" + accuDriftSeconds);
	}

	@Override
	public void tickCompleted () {
		super.tickCompleted();

		if (hasPlayer()) {
			float pl = gameTrack.getTrackCompletion(playerCar);
			playerTasks.hudPlayer.trackProgress.setPlayerProgression(pl);

			float distNorm = 0;
			float distMt = 0;
			float ghSpeed = 0;

			// use the last one if the replay is finished
			if (nextTarget != null && nextTarget.hasReplay()) {
				lastDist = gameTrack.getTrackDistance(nextTarget);
				lastCompletion = gameTrack.getTrackCompletion(nextTarget);
				ghSpeed = nextTarget.getInstantSpeed();
			}

			distNorm = pl - lastCompletion;
			distMt = gameTrack.getTrackDistance(playerCar) - lastDist;
			playerTasks.hudPlayer.trackProgress.setDistanceFromBest(distNorm);

			playerTasks.hudPlayer.trackProgress.setPlayerSpeed(playerCar.getInstantSpeed());
			playerTasks.hudPlayer.trackProgress.setPlayerDistance(gameTrack.getTrackDistance(playerCar));

			playerTasks.hudPlayer.trackProgress.setGhostSpeed(ghSpeed);
			playerTasks.hudPlayer.trackProgress.setGhostDistance(lastDist);

			// target tracker
			float alpha = MathUtils.clamp(Math.abs(distMt) / 50, 0.2f, 1);
			playerTasks.hudPlayer.setNextTargetAlpha(alpha);
		}
	}

	@Override
	protected void discardedReplay (Replay replay) {
	}

	// the player begins drifting
	@Override
	public void driftBegins () {
	}

	// the player's drift ended
	@Override
	public void driftEnds () {
	}

	@Override
	protected boolean timeDilationAvailable () {
		return accuDriftSeconds.value > 0;
	}

	// the player begins slowing down time
	@Override
	public void timeDilationBegins () {
		dilationTime.start();
		driftBar.showSecondsLabel();
	}

	// the player ends slowing down time
	@Override
	public void timeDilationEnds () {
		updateDriftBar();
		dilationTime.reset();
		driftBar.hideSecondsLabel();
	}

	private TweenCallback penaltyFinished = new TweenCallback() {
		@Override
		public void onEvent (int type, BaseTween<?> source) {
			switch (type) {
			case COMPLETE:
				isPenalty = false;
			}
		}
	};

	@Override
	protected void collision () {
		if (isPenalty) return;

		isPenalty = true;

		GameTweener.stop(accuDriftSeconds);
		driftSecondsTimeline = Timeline.createSequence();
		driftSecondsTimeline.push(Tween.to(accuDriftSeconds, BoxedFloatAccessor.VALUE, 500).target(0).ease(Quad.INOUT))
			.setCallback(penaltyFinished);
		GameTweener.start(driftSecondsTimeline);

		playerTasks.hudPlayer.highlightCollision();
	}

	@Override
	protected void outOfTrack () {
		outOfTrackTime.start();
		driftBar.showSecondsLabel();
	}

	@Override
	protected void backInTrack () {
		updateDriftBar();
		outOfTrackTime.reset();
		driftBar.hideSecondsLabel();
	}

	@Override
	protected void lapComplete (boolean firstLap) {
		restartAllReplays();
		playerTasks.hudPlayer.trackProgress.lapCompleted();
	}

	@Override
	protected void ghostFadingOut (Car ghost) {
		if (hasPlayer() && ghost == nextTarget) {
			playerTasks.hudPlayer.unHighlightNextTarget(ghost);
		}
	}

	//
	// utilities
	//

	private GhostCar nextTarget = null;

	private void restartAllReplays () {
		Array<Replay> replays = replayManager.getReplays();

		nextTarget = null;
		lastDist = 0;
		lastCompletion = 0;

		for (int i = 0; i < replays.size; i++) {
			Replay r = replays.get(i);
			if (r.isValid) {
				getGhost(i).setReplay(replays.get(i));

				if (replayManager.getBestReplay() == replays.get(i)) {
					nextTarget = getGhost(i);
					playerTasks.hudPlayer.highlightNextTarget(nextTarget);
				}
			}
		}
	}

	private void updateDriftBar () {
		if (!hasPlayer()) {
			return;
		}

		if (Config.Debug.InfiniteDilationTime) {
			accuDriftSeconds.value = DriftBar.MaxSeconds;
		} else {

			// if a penalty is being applied, then no drift seconds will be counted
			if (!isPenalty) {

				// earn game seconds by drifting
				if (playerCar.driftState.isDrifting) {
					accuDriftSeconds.value += Config.Physics.PhysicsDt;
				}

				// lose wall-clock seconds while in time dilation
				if (!dilationTime.isStopped()) {
					accuDriftSeconds.value -= dilationTime.elapsed(Reference.LastAbsoluteSeconds);
				}

				// lose wall-clock seconds while out of track
				if (!outOfTrackTime.isStopped()) {
					accuDriftSeconds.value -= outOfTrackTime.elapsed(Reference.LastAbsoluteSeconds);
				}

				accuDriftSeconds.value = MathUtils.clamp(accuDriftSeconds.value, 0, DriftBar.MaxSeconds);
			}
		}

		driftBar.setSeconds(accuDriftSeconds.value);
	}
}
