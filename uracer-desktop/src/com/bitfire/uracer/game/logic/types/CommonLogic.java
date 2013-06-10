
package com.bitfire.uracer.game.logic.types;

import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.equations.Quad;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.MathUtils;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.events.CarEvent;
import com.bitfire.uracer.events.GhostCarEvent;
import com.bitfire.uracer.events.PlayerDriftStateEvent;
import com.bitfire.uracer.game.DebugHelper;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.GameInput;
import com.bitfire.uracer.game.GameLogic;
import com.bitfire.uracer.game.Time;
import com.bitfire.uracer.game.Time.Reference;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.CarPreset;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.logic.GameTasksManager;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.HudPlayer.EndDriftType;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.player.DriftBar;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.player.TrackProgress;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.player.TrackProgress.TrackProgressData;
import com.bitfire.uracer.game.logic.helpers.CarFactory;
import com.bitfire.uracer.game.logic.helpers.GameTrack;
import com.bitfire.uracer.game.logic.helpers.PlayerGameTasks;
import com.bitfire.uracer.game.logic.post.PostProcessing;
import com.bitfire.uracer.game.logic.replaying.LapManager;
import com.bitfire.uracer.game.logic.replaying.Replay;
import com.bitfire.uracer.game.logic.replaying.ReplayManager;
import com.bitfire.uracer.game.logic.replaying.ReplayRecorder.RecorderError;
import com.bitfire.uracer.game.logic.types.helpers.LapCompletionMonitor;
import com.bitfire.uracer.game.logic.types.helpers.LapCompletionMonitor.LapCompletionMonitorListener;
import com.bitfire.uracer.game.logic.types.helpers.TimeModulator;
import com.bitfire.uracer.game.logic.types.helpers.WrongWayMonitor;
import com.bitfire.uracer.game.logic.types.helpers.WrongWayMonitor.WrongWayMonitorListener;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.rendering.GameWorldRenderer;
import com.bitfire.uracer.game.screens.GameScreensFactory.ScreenType;
import com.bitfire.uracer.game.tween.GameTweener;
import com.bitfire.uracer.game.tween.SysTweener;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.screen.TransitionFactory.TransitionType;
import com.bitfire.uracer.utils.BoxedFloat;
import com.bitfire.uracer.utils.BoxedFloatAccessor;
import com.bitfire.uracer.utils.CarUtils;
import com.bitfire.uracer.utils.NumberString;

public abstract class CommonLogic implements GameLogic {

	protected abstract float updateCamera (float timeModFactor);

	protected void newReplay (Replay replay) {
	}

	protected void discardedReplay (Replay replay) {
	}

	protected void lapStarted () {
	}

	protected void lapCompleted () {
	}

	protected void warmUpStarted () {

	}

	protected void warmUpCompleted () {
	}

	protected void driftBegins () {
	}

	protected void driftEnds () {
	}

	protected void outOfTrack () {
	}

	protected void backInTrack () {
	}

	// input
	protected Input inputSystem = null;
	protected GameInput input = null;

	// world
	protected GameWorld gameWorld = null;
	protected GameTrack gameTrack = null;

	// rendering
	private GameRenderer gameRenderer = null;
	protected GameWorldRenderer gameWorldRenderer = null;
	protected PostProcessing postProcessing = null;

	// player
	protected final EventHandlers eventHandlers = new EventHandlers();
	protected final UserProfile userProfile;
	protected PlayerCar playerCar = null;
	protected GhostCar[] ghostCars = new GhostCar[ReplayManager.MaxReplays];
	private WrongWayMonitor wrongWayMonitor;
	protected boolean isCurrentLapValid = true;
	protected boolean isTooSlow = false;
	protected boolean isPenalty;
	private GhostCar nextTarget = null;
	private Time dilationTime = new Time();
	private Time outOfTrackTime = new Time();

	// lap / replays
	protected LapManager lapManager = null;
	private LapCompletionMonitor lapMonitor = null;

	// tasks
	protected GameTasksManager gameTasksManager = null;
	protected PlayerGameTasks playerTasks = null;

	// time modulation logic
	private TimeModulator timeMod = null;

	private BoxedFloat accuDriftSeconds = new BoxedFloat(0);;

	public CommonLogic (UserProfile userProfile, GameWorld gameWorld, GameRenderer gameRenderer) {
		this.userProfile = userProfile;
		this.gameWorld = gameWorld;
		this.gameRenderer = gameRenderer;
		this.gameWorldRenderer = gameRenderer.getWorldRenderer();
		this.inputSystem = URacer.Game.getInputSystem();
		timeMod = new TimeModulator();

		// post-processing
		postProcessing = gameRenderer.getPostProcessing();

		// create game and player tasks
		gameTasksManager = new GameTasksManager(gameWorld);
		playerTasks = new PlayerGameTasks(userProfile, gameTasksManager);

		lapManager = new LapManager(userProfile, gameWorld.getLevelId());
		for (int i = 0; i < ReplayManager.MaxReplays; i++) {
			ghostCars[i] = CarFactory.createGhost(i, gameWorld, CarPreset.Type.L1_GoblinOrange);
		}

		eventHandlers.registerGhostEvents();

		gameWorldRenderer.setGhostCars(ghostCars);
		gameTrack = gameWorld.getGameTrack();

		wrongWayMonitor = new WrongWayMonitor(eventHandlers);
		lapMonitor = new LapCompletionMonitor(eventHandlers, gameTrack);

		input = new GameInput(this, inputSystem);
	}

	@Override
	public void dispose () {
		removePlayer();
		gameTasksManager.dispose();
		playerTasks.dispose();

		if (playerCar != null) {
			playerCar.dispose();
		}

		for (int i = 0; i < ReplayManager.MaxReplays; i++) {
			if (ghostCars[i] != null) {
				ghostCars[i].dispose();
			}
		}

		eventHandlers.unregisterGhostEvents();

		lapManager.dispose();
		GameTweener.dispose();
	}

	/** Sets the player from the specified preset */
	@Override
	public void addPlayer () {
		if (hasPlayer()) {
			Gdx.app.log("GameLogic", "A player already exists.");
			return;
		}

		playerCar = CarFactory.createPlayer(gameWorld);
		gameWorld.setPlayer(playerCar);

		configurePlayer(gameWorld, inputSystem, playerCar);
		Gdx.app.log("GameLogic", "Player configured");

		playerTasks.createTasks(playerCar, lapManager, gameRenderer);
		Gdx.app.log("GameLogic", "Game tasks created and configured");

		eventHandlers.registerPlayerEvents();
		Gdx.app.log("GameLogic", "Registered player-related events");

		postProcessing.setPlayer(playerCar);
		gameWorldRenderer.setPlayerCar(playerCar);
		gameWorldRenderer.setRenderPlayerHeadlights(gameWorld.isNightMode());

		gameWorldRenderer.setInitialCameraPositionOrient(playerCar);
		updateCamera(0);
		gameWorldRenderer.updateCamera();

		gameWorldRenderer.showDebugGameTrack(Config.Debug.RenderTrackSectors);
		gameWorldRenderer.setGameTrackDebugCar(playerCar);

		restartGame();

		if (Config.Debug.UseDebugHelper) {
			DebugHelper.setPlayer(playerCar);
		}
	}

	@Override
	public void removePlayer () {
		if (!hasPlayer()) {
			Gdx.app.log("GameLogic", "There is no player to remove.");
			return;
		}

		// setting a null player (disabling player), unregister
		// previously registered events, if there was a player
		if (playerCar != null) {
			eventHandlers.unregisterPlayerEvents();
			playerTasks.destroyTasks();
			playerCar.dispose();
		}

		playerCar = null;
		gameWorld.setPlayer(null);
		gameWorldRenderer.setRenderPlayerHeadlights(false);
		wrongWayMonitor.reset();
		lapMonitor.reset(null);

		if (Config.Debug.UseDebugHelper) {
			DebugHelper.setPlayer(null);
		}
	}

	/** Restarts the current game */
	@Override
	public void restartGame () {
		restartLogic();
	}

	/** Restart and completely resets the game, removing any previous recording and playing replays */
	@Override
	public void resetGame () {
		resetLogic();
	}

	@Override
	public void quitGame () {
		lapManager.abortRecording();
		gameTasksManager.sound.stop();

		URacer.Screens.setScreen(ScreenType.MainScreen, TransitionType.Fader, 500);
		// URacer.Screens.setScreen( ScreenType.ExitScreen, TransitionType.Fader, 500 );

		timeMod.reset();
		URacer.Game.resetTimeModFactor();
	}

	/** Request time dilation to begin */
	@Override
	public void startTimeDilation () {
		dilationTime.start();
		timeMod.toDilatedTime();
		playerTasks.hudPlayer.driftBar.showSecondsLabel();
	}

	/** Request time dilation to end */
	@Override
	public void endTimeDilation () {
		input.resetTimeDilating();
		dilationTime.reset();
		timeMod.toNormalTime();
		playerTasks.hudPlayer.driftBar.hideSecondsLabel();
	}

	@Override
	public boolean isTimeDilationAvailable () {
		return accuDriftSeconds.value > 0;
	}

	@Override
	public void tick () {
		// compute the next-frame time multiplier
		URacer.timeMultiplier = timeMod.getTime();
		input.update();
		dbgInput();
	}

	@Override
	public void tickCompleted () {
		updateLogic();
	}

	@Override
	public void beforeRender () {
		float zoom = updateCamera(URacer.Game.getTimeModFactor());
		gameWorldRenderer.updateCamera();
		postProcessing.onBeforeRender(zoom, lapMonitor.getWarmUpCompletion());

		// game tweener step
		GameTweener.update();
	}

	private float lastDist, lastCompletion;

	private void updateLogic () {
		// FIXME add more description to WHY things are in this order
		if (hasPlayer()) {
			gameTrack.updateTrackState(playerCar);
		}

		for (int i = 0; i < ReplayManager.MaxReplays; i++) {
			if (ghostCars[i] != null && ghostCars[i].hasReplay()) {
				gameTrack.updateTrackState(ghostCars[i]);
			}
		}

		if (hasPlayer()) {
			// triggers wrong way event callbacks
			wrongWayMonitor.update(gameTrack.getTrackRouteConfidence(playerCar));

			isCurrentLapValid = !wrongWayMonitor.isWrongWay() && !isTooSlow;

			if (wrongWayMonitor.isWrongWay() || isTooSlow) {
				// blink CarHighlighter on wrong way or too slow (keeps calling, returns earlier if busy)
				playerTasks.hudPlayer.highlightWrongWay();

				// inhibits lap monitor to raise events
				lapMonitor.reset();
			} else {
				// triggers lap event callbacks
				lapMonitor.update();
			}
		}

		{
			// blink CarHighlighter on out of track (keeps calling, returns earlier if busy)
			if (playerCar.isOutOfTrack()) {
				playerTasks.hudPlayer.highlightOutOfTrack();
			}

			// reset progress if too slow
			if (isTooSlow) {
				playerTasks.hudPlayer.trackProgress.getProgressData().reset(true);
			}
		}

		// ends time dilation if no more seconds available
		if (accuDriftSeconds.value == 0 && input.isTimeDilating()) {
			endTimeDilation();
			Gdx.app.log("CommonLogic", "Requesting time modulation to finish");
		}

		if (hasPlayer()) {
			updateDriftBar();

			TrackProgress progress = playerTasks.hudPlayer.trackProgress;
			TrackProgressData data = progress.getProgressData();

			if (lapMonitor.isWarmUp()) {
				data.reset(true);
				if (isCurrentLapValid) {
					progress.setMessage("Start in "
						+ Math.round(gameTrack.getTotalLength() - gameTrack.getTrackDistance(playerCar, 0)) + " mt");
				} else {
					progress.setMessage("Press \"R\"\nto restart");
				}
			} else {
				if (isCurrentLapValid) {
					progress.setMessage("");

					// use the last one if the replay is finished
					if (nextTarget != null && nextTarget.hasReplay()) {
						lastDist = gameTrack.getTrackDistance(nextTarget, 0);
						lastCompletion = gameTrack.getTrackCompletion(nextTarget);
					}

					data.setPlayerDistance(gameTrack.getTrackDistance(playerCar, 0));
					data.setPlayerProgression(gameTrack.getTrackCompletion(playerCar));

					data.setTargetDistance(lastDist);
					data.setTargetProgression(lastCompletion);

					// target tracker
					float distMt = gameTrack.getTrackDistance(playerCar, 0) - lastDist;
					float alpha = MathUtils.clamp(Math.abs(distMt) / 50, 0.2f, 1);
					playerTasks.hudPlayer.setNextTargetAlpha(alpha);

				} else {
					progress.setMessage("Press \"R\"\nto restart");
					data.reset(true);
				}
			}
		}
	}

	public GameWorld getGameWorld () {
		return gameWorld;
	}

	public boolean hasPlayer () {
		return playerCar != null;
	}

	//
	// private implementation
	//

	private void configurePlayer (GameWorld world, Input inputSystem, PlayerCar player) {
		// create and setup the player
		player.setInputSystem(inputSystem);
		player.setFrictionMap(Art.frictionMapDesert);
		player.setWorldPosMt(world.playerStart.position, world.playerStart.orientation);
		player.resetPhysics();
	}

	private void resetPlayer (GameWorld world, Car playerCar) {
		if (playerCar != null) {
			playerCar.resetPhysics();
			// playerCar.getTrackState().reset();
			playerCar.resetDistanceAndSpeed(true, true);
			playerCar.setWorldPosMt(world.playerStart.position, world.playerStart.orientation);
		}
	}

	protected GhostCar getGhost (int handle) {
		return ghostCars[handle];
	}

	protected boolean isGhostActive (int handle) {
		return (ghostCars[handle] != null && ghostCars[handle].isActive());
	}

	protected void setGhostReplay (int ghost, Replay replay) {
		GhostCar ghostcar = ghostCars[ghost];
		ghostcar.setReplay(replay);
		gameTrack.resetTrackState(ghostcar);
	}

	private void resetGhost (int handle) {
		GhostCar ghost = ghostCars[handle];
		if (ghost != null) {
			// ghost.getTrackState().reset();
			ghost.resetPhysics();
			ghost.resetDistanceAndSpeed(true, true);
			ghost.removeReplay();
		}
	}

	protected void resetAllGhosts () {
		for (int i = 0; i < ReplayManager.MaxReplays; i++) {
			resetGhost(i);
		}
	}

	protected void restartAllReplays () {
		nextTarget = null;
		lastDist = 0;

		int ghostIndex = 0;
		for (Replay r : lapManager.getReplays()) {
			if (r.isValid()) {
				setGhostReplay(ghostIndex, r);

				if (lapManager.getBestReplay() == r) {
					nextTarget = ghostCars[ghostIndex];
					playerTasks.hudPlayer.highlightNextTarget(nextTarget);
				}

				ghostIndex++;
			}
		}
	}

	private void restartLogic () {
		SysTweener.clear();
		GameTweener.clear();

		gameTrack.clearTrackStates();
		resetPlayer(gameWorld, playerCar);
		resetAllGhosts();

		dilationTime.reset();
		outOfTrackTime.reset();
		input.resetTimeDilating();
		timeMod.reset();
		lapManager.abortRecording();
		gameTasksManager.raiseRestart();
		wrongWayMonitor.reset();
		postProcessing.resetAnimator();
		lapMonitor.reset(playerCar);

		accuDriftSeconds.value = 0;
		lastDist = 0;
		lastCompletion = 0;
		isCurrentLapValid = true;
		isTooSlow = false;
		isPenalty = false;
	}

	private void resetLogic () {
		restartLogic();

		// clean everything
		lapManager.removeAllReplays();
		lapManager.reset();
		gameTasksManager.raiseReset();
	}

	private void updateDriftBar () {
		if (!hasPlayer()) {
			return;
		}

		DriftBar driftBar = playerTasks.hudPlayer.driftBar;
		driftBar.setDriftStrength(playerCar.driftState.driftStrength);

		if (Config.Debug.InfiniteDilationTime) {
			accuDriftSeconds.value = DriftBar.MaxSeconds;
		} else {

			// earn game-time seconds by drifting
			if (playerCar.driftState.isDrifting) {
				accuDriftSeconds.value += Config.Physics.Dt + Config.Physics.Dt * playerCar.driftState.driftStrength;
			}

			// lose wall-clock seconds while in time dilation
			if (!dilationTime.isStopped()) {
				accuDriftSeconds.value -= dilationTime.elapsed(Reference.LastAbsoluteSeconds) * 2;
			}

			// lose wall-clock seconds while out of track
			if (!outOfTrackTime.isStopped()) {
				accuDriftSeconds.value -= outOfTrackTime.elapsed(Reference.LastAbsoluteSeconds);
			}
		}

		accuDriftSeconds.value = MathUtils.clamp(accuDriftSeconds.value, 0, DriftBar.MaxSeconds);
		driftBar.setSeconds(accuDriftSeconds.value);
	}

	private Replay userRec = null; // dbg on-demand rec/play via Z/X

	private void dbgInput () {
		if (inputSystem.isPressed(Keys.O)) {
			removePlayer();
		} else if (inputSystem.isPressed(Keys.P)) {
			addPlayer();
		} else if (inputSystem.isPressed(Keys.W)) {
			Config.Debug.RenderBox2DWorldWireframe = !Config.Debug.RenderBox2DWorldWireframe;
		} else if (inputSystem.isPressed(Keys.B)) {
			Config.Debug.Render3DBoundingBoxes = !Config.Debug.Render3DBoundingBoxes;
		} else if (inputSystem.isPressed(Keys.TAB)) {
			Config.Debug.RenderTrackSectors = !Config.Debug.RenderTrackSectors;
			gameWorldRenderer.showDebugGameTrack(Config.Debug.RenderTrackSectors);
			gameWorldRenderer.setGameTrackDebugCar(playerCar);
		} else if (inputSystem.isPressed(Keys.Z)) {

			// start recording
			playerCar.resetDistanceAndSpeed(true, true);
			resetAllGhosts();
			lapManager.abortRecording();
			userRec = lapManager.startRecording(playerCar);
			Gdx.app.log("GameLogic", "Recording...");

		} else if (inputSystem.isPressed(Keys.X)) {

			// stop recording and play
			playerCar.resetPhysics();
			lapManager.stopRecording();

			CarUtils.dumpSpeedInfo("Player", playerCar, lapManager.getLastRecordedReplay().getTrackTime());
			playerCar.resetDistanceAndSpeed(true, true);
			if (userRec != null) {
				userRec.saveLocal(gameTasksManager.messager);
				setGhostReplay(0, userRec);
			}

			// Gdx.app.log( "GameLogic", "Player final pos=" +
			// playerCar.getBody().getPosition() );

		} else if (inputSystem.isPressed(Keys.L)) {
			playerCar.resetPhysics();
			playerCar.resetDistanceAndSpeed(true, true);
			lapManager.stopRecording();
			setGhostReplay(0, Replay.loadLocal("test-replay"));
		} else if (inputSystem.isPressed(Keys.K)) {
			playerCar.resetPhysics();
			playerCar.resetDistanceAndSpeed(true, true);
			lapManager.stopRecording();
			setGhostReplay(0, Replay.loadLocal("test-replay-coll"));
		} else if (inputSystem.isPressed(Keys.W)) {
			Config.Debug.RenderBox2DWorldWireframe = !Config.Debug.RenderBox2DWorldWireframe;
		} else if (inputSystem.isPressed(Keys.B)) {
			Config.Debug.Render3DBoundingBoxes = !Config.Debug.Render3DBoundingBoxes;
		} else if (inputSystem.isPressed(Keys.TAB)) {
			Config.Debug.RenderTrackSectors = !Config.Debug.RenderTrackSectors;
			gameWorldRenderer.showDebugGameTrack(Config.Debug.RenderTrackSectors);
			gameWorldRenderer.setGameTrackDebugCar(playerCar);
		}
	}

	private final class EventHandlers implements WrongWayMonitorListener, LapCompletionMonitorListener {

		// LapCompletionMonitorListener events order redux
		//
		// 1. warmup started
		// 2. warmup completed + 3. lap started
		// 4. lap completed + 5. lap started

		@Override
		public void onWarmUpStarted () {
			Gdx.app.log("CommonLogic", "Warmup Started");
			warmUpStarted();
		}

		@Override
		public void onWarmUpCompleted () {
			playerTasks.hudPlayer.trackProgress.getProgressData().reset(true);
			Gdx.app.log("CommonLogic", "Warmup Completed");

			warmUpCompleted();
		}

		@Override
		public void onLapStarted () {
			Gdx.app.log("CommonLogic", "Lap Started");

			lapManager.stopRecording();
			playerCar.resetDistanceAndSpeed(true, false);
			lapManager.startRecording(playerCar);

			restartAllReplays();
			playerTasks.hudPlayer.trackProgress.getProgressData().reset(false);

			lapStarted();
		}

		@Override
		public void onLapCompleted () {
			Gdx.app.log("CommonLogic", "Lap Completed");
			lapManager.stopRecording();

			if (!isCurrentLapValid) {
				return;
			}

			// always work on the ReplayManager copy!
			Replay lastRecorded = lapManager.getLastRecordedReplay();
			Replay replay = lapManager.addReplay(lastRecorded);
			if (replay != null) {
				newReplay(replay);
			} else {
				if (lastRecorded != null && lastRecorded.isValid()) {
					discardedReplay(lastRecorded);
				}
			}

			playerCar.resetDistanceAndSpeed(true, false);
			lapCompleted();
		}

		@Override
		public void onWrongWayBegins () {
			lapManager.abortRecording();

			playerTasks.hudPlayer.wrongWay.fadeIn();
			playerTasks.hudLapInfo.toColor(1, 0, 0);
			playerTasks.hudLapInfo.setInvalid("invalid lap");
			postProcessing.alertWrongWayBegins(500);
		}

		@Override
		public void onWrongWayEnds () {
		}

		PlayerDriftStateEvent.Listener driftStateListener = new PlayerDriftStateEvent.Listener() {
			@Override
			public void handle (Object source, PlayerDriftStateEvent.Type type, PlayerDriftStateEvent.Order order) {
				PlayerCar player = (PlayerCar)source;

				switch (type) {
				case onBeginDrift:
					playerTasks.hudPlayer.beginDrift();
					driftBegins();
					break;
				case onEndDrift:
					driftEnds();

					float driftSeconds = player.driftState.driftSeconds();
					// String msgSeconds = NumberString.format(playerCar.driftState.driftSeconds()) + "  seconds!";

					if (player.driftState.hasCollided) {
						playerTasks.hudPlayer.endDrift("-" + NumberString.format(driftSeconds), EndDriftType.BadDrift);
					} else {

						// if (driftSeconds >= 1 && driftSeconds < 3f) {
						// gameTasksManager.messager.enqueue("NICE ONE!\n+" + msgSeconds, 1f, Type.Good, Position.Bottom, Size.Big);
						// } else if (driftSeconds >= 3f && driftSeconds < 5f) {
						// gameTasksManager.messager.enqueue("FANTASTIC!\n+" + msgSeconds, 1f, Type.Good, Position.Bottom, Size.Big);
						// } else if (driftSeconds >= 5f) {
						// gameTasksManager.messager.enqueue("UNREAL!\n+" + msgSeconds, 1f, Type.Good, Position.Bottom, Size.Big);
						// }

						playerTasks.hudPlayer.endDrift("+" + NumberString.format(driftSeconds), EndDriftType.GoodDrift);
					}

					break;
				}
			}
		};

		CarEvent.Listener playerCarListener = new CarEvent.Listener() {
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
			public void handle (Object source, CarEvent.Type type, CarEvent.Order order) {
				CarEvent.Data eventData = GameEvents.playerCar.data;

				switch (type) {
				case onCollision:

					// invalidate time modulation
					if (input.isTimeDilating()) {
						endTimeDilation();
					}

					postProcessing.alertCollision(0.75f, 4000);

					if (!isPenalty) {
						isPenalty = true;
						GameTweener.stop(accuDriftSeconds);
						GameTweener.start(Timeline.createSequence()
							.push(Tween.to(accuDriftSeconds, BoxedFloatAccessor.VALUE, 500).target(0).ease(Quad.INOUT))
							.setCallback(penaltyFinished));
						playerTasks.hudPlayer.highlightCollision();
					}

					break;
				case onOutOfTrack:
					outOfTrackTime.start();
					playerTasks.hudPlayer.driftBar.showSecondsLabel();

					outOfTrack();
					break;
				case onBackInTrack:
					// updateDriftBar();
					outOfTrackTime.reset();
					playerTasks.hudPlayer.driftBar.hideSecondsLabel();

					backInTrack();
					break;
				case onComputeForces:
					RecorderError recerror = lapManager.record(eventData.forces);
					if (recerror == RecorderError.ReplayMemoryLimitReached) {
						Gdx.app.log("CommonLogic", "Player too slow, recording aborted.");
						isTooSlow = true;
						lapManager.abortRecording();
						playerTasks.hudLapInfo.setInvalid("Too slow!");
						playerTasks.hudLapInfo.toColor(1, 0, 0);
					}
					// RecordingNotEnabled
					// NoError

					break;
				}
			}
		};

		GhostCarEvent.Listener ghostListener = new GhostCarEvent.Listener() {
			@Override
			public void handle (Object source, GhostCarEvent.Type type, GhostCarEvent.Order order) {
				switch (type) {
				case onGhostFadingOut:
					if (hasPlayer() && source == nextTarget) {
						playerTasks.hudPlayer.unHighlightNextTarget();
					}
					break;
				}
			}

		};

		public void registerPlayerEvents () {
			GameEvents.driftState.addListener(driftStateListener, PlayerDriftStateEvent.Type.onBeginDrift);
			GameEvents.driftState.addListener(driftStateListener, PlayerDriftStateEvent.Type.onEndDrift);

			GameEvents.playerCar.addListener(playerCarListener, CarEvent.Type.onCollision);
			GameEvents.playerCar.addListener(playerCarListener, CarEvent.Type.onComputeForces);
			GameEvents.playerCar.addListener(playerCarListener, CarEvent.Type.onOutOfTrack);
			GameEvents.playerCar.addListener(playerCarListener, CarEvent.Type.onBackInTrack);
		}

		public void unregisterPlayerEvents () {
			GameEvents.driftState.removeListener(driftStateListener, PlayerDriftStateEvent.Type.onBeginDrift);
			GameEvents.driftState.removeListener(driftStateListener, PlayerDriftStateEvent.Type.onEndDrift);

			GameEvents.playerCar.removeListener(playerCarListener, CarEvent.Type.onCollision);
			GameEvents.playerCar.removeListener(playerCarListener, CarEvent.Type.onComputeForces);
			GameEvents.playerCar.removeListener(playerCarListener, CarEvent.Type.onOutOfTrack);
			GameEvents.playerCar.removeListener(playerCarListener, CarEvent.Type.onBackInTrack);
		}

		public void registerGhostEvents () {
			GameEvents.ghostCars.addListener(ghostListener, GhostCarEvent.Type.onGhostFadingOut);
		}

		public void unregisterGhostEvents () {
			GameEvents.ghostCars.removeListener(ghostListener, GhostCarEvent.Type.onGhostFadingOut);
		}
	}
}
