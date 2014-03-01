
package com.bitfire.uracer.game.logic.types;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.GameInput;
import com.bitfire.uracer.game.GameLogic;
import com.bitfire.uracer.game.GameLogicObserver;
import com.bitfire.uracer.game.Time;
import com.bitfire.uracer.game.actors.CarPreset.Type;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.events.GameLogicEvent;
import com.bitfire.uracer.game.logic.gametasks.GameTasksManager;
import com.bitfire.uracer.game.logic.gametasks.Messager;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.player.DriftBar;
import com.bitfire.uracer.game.logic.gametasks.messager.Message;
import com.bitfire.uracer.game.logic.gametasks.messager.Message.Position;
import com.bitfire.uracer.game.logic.gametasks.messager.Message.Size;
import com.bitfire.uracer.game.logic.helpers.CarFactory;
import com.bitfire.uracer.game.logic.helpers.GameTrack;
import com.bitfire.uracer.game.logic.helpers.PlayerGameTasks;
import com.bitfire.uracer.game.logic.helpers.TrackProgressData;
import com.bitfire.uracer.game.logic.post.PostProcessing;
import com.bitfire.uracer.game.logic.replaying.LapManager;
import com.bitfire.uracer.game.logic.replaying.ReplayManager;
import com.bitfire.uracer.game.logic.types.helpers.GhostLapCompletionMonitor;
import com.bitfire.uracer.game.logic.types.helpers.PlayerLapCompletionMonitor;
import com.bitfire.uracer.game.logic.types.helpers.WrongWayMonitor;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.rendering.GameWorldRenderer;
import com.bitfire.uracer.game.tween.GameTweener;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.utils.BoxedFloat;

public abstract class CommonLogic implements GameLogic, GameLogicObserver {
	// input
	protected Input inputSystem = null;
	protected GameInput gameInput = null;
	protected boolean quitPending = false, quitScheduled = false, paused = false;

	// world
	protected GameWorld gameWorld = null;
	protected GameRenderer gameRenderer = null;
	protected GameTrack gameTrack = null;

	// rendering
	protected GameWorldRenderer gameWorldRenderer = null;
	protected PostProcessing postProcessing = null;

	// player
	protected final EventHandlers eventHandlers;
	protected final UserProfile userProfile;
	protected PlayerCar playerCar = null;
	protected GhostCar[] ghostCars = new GhostCar[ReplayManager.MaxReplays];
	private WrongWayMonitor wrongWayMonitor;
	protected boolean isCurrentLapValid = true;

	// lap / replays
	protected LapManager lapManager = null;
	protected PlayerLapCompletionMonitor playerLapMonitor = null;
	protected GhostLapCompletionMonitor[] ghostLapMonitor = new GhostLapCompletionMonitor[ReplayManager.MaxReplays];
	protected TrackProgressData progressData = new TrackProgressData();

	// tasks
	protected GameTasksManager gameTasksManager = null;
	protected PlayerGameTasks playerTasks = null;
	protected Messager messager = null;

	private BoxedFloat accuDriftSeconds = new BoxedFloat(DriftBar.MaxSeconds);

	public CommonLogic (UserProfile userProfile, GameWorld gameWorld, GameRenderer gameRenderer) {
		this.userProfile = userProfile;
		this.gameWorld = gameWorld;
		this.gameRenderer = gameRenderer;
		this.gameWorldRenderer = gameRenderer.getWorldRenderer();
		this.inputSystem = URacer.Game.getInputSystem();
		this.gameTrack = gameWorld.getGameTrack();
		this.messager = new Messager();
		this.eventHandlers = new EventHandlers(this);

		lapManager = new LapManager(gameWorld.getLevelId());

		// post-processing
		postProcessing = gameRenderer.getPostProcessing();

		// create both game and player tasks
		gameTasksManager = new GameTasksManager(gameWorld, postProcessing.getPostProcessor());
		playerTasks = new PlayerGameTasks(userProfile, gameTasksManager);
		playerTasks.createTasks(lapManager, progressData);

		// create ghost cars and provides them via interface @Overrides
		for (int i = 0; i < ghostCars.length; i++) {
			ghostCars[i] = CarFactory.createGhost(i, gameWorld, Type.Default);
			ghostLapMonitor[i] = new GhostLapCompletionMonitor(gameTrack);
			ghostLapMonitor[i].reset();
		}

		gameWorld.setGhostCars(ghostCars);

		// register events
		eventHandlers.registerGhostEvents();
		eventHandlers.registerRenderEvents();

		// create player monitors and setup listeners
		wrongWayMonitor = new WrongWayMonitor();
		playerLapMonitor = new PlayerLapCompletionMonitor(gameTrack);

		// set progress data
		playerTasks.hudPlayer.trackProgress.setTrackProgressData(progressData);

		// create game input
		gameInput = new GameInput(this, inputSystem);
	}

	//
	// GameLogic impl
	//

	@Override
	public void dispose () {
		removePlayer();
		gameTasksManager.dispose();
		playerTasks.dispose();

		if (playerCar != null) {
			playerCar.dispose();
		}

		for (int i = 0; i < ghostCars.length; i++) {
			if (ghostCars[i] != null) {
				ghostCars[i].dispose();
			}
		}

		eventHandlers.unregisterGhostEvents();
		eventHandlers.unregisterRenderEvents();

		lapManager.dispose();
		GameTweener.dispose();
	}

	@Override
	public boolean hasPlayer () {
		return playerCar != null;
	}

	@Override
	public UserProfile getUserProfile () {
		return userProfile;
	}

	@Override
	public GhostCar[] getGhosts () {
		return ghostCars;
	}

	@Override
	public GhostCar getGhost (int handle) {
		return ghostCars[handle];
	}

	@Override
	public boolean isGhostActive (int handle) {
		return (ghostCars[handle] != null && ghostCars[handle].isActive());
	}

	@Override
	public boolean isWarmUp () {
		return hasPlayer() && playerLapMonitor.isWarmUp();
	}

	@Override
	public void addPlayer () {
		if (hasPlayer()) {
			Gdx.app.log("GameLogic", "A player already exists.");
			return;
		}

		playerCar = CarFactory.createPlayer(gameWorld, this, Type.Car_Yellow);
		playerCar.setInputSystem(inputSystem);
		playerCar.setFrictionMap(Art.frictionMapDesert);
		playerCar.reset();
		Gdx.app.log("GameLogic", "Player configured");

		GameEvents.logicEvent.player = playerCar;
		GameEvents.logicEvent.trigger(this, GameLogicEvent.Type.PlayerAdded);

		Gdx.app.log("GameLogic", "Game tasks created and configured");

		eventHandlers.registerPlayerEvents();
		eventHandlers.registerPlayerMonitorEvents();
		Gdx.app.log("GameLogic", "Registered player-related events");

		postProcessing.setPlayer(playerCar);
		playerLapMonitor.reset();
		gameWorld.setPlayer(playerCar);
		gameWorldRenderer.setRenderPlayerHeadlights(gameWorld.isNightMode());
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
			eventHandlers.unregisterPlayerMonitorEvents();
			playerCar.dispose();
			playerCar = null;
			GameEvents.logicEvent.player = null;
			GameEvents.logicEvent.trigger(this, GameLogicEvent.Type.PlayerRemoved);
		}

		gameWorld.setPlayer(null);
		gameWorldRenderer.setRenderPlayerHeadlights(false);
		wrongWayMonitor.reset();
		lapManager.reset(true);
		playerLapMonitor.reset();
		progressData.reset(true);
		progressData.resetLogicStates();
		postProcessing.setPlayer(null);
	}

	private void realRestart () {
		if (hasPlayer()) playerCar.reset();
		endTimeDilation();

		getOutOfTrackTimer().reset();
		endCollisionTime();
		lapManager.abortRecording(true);
		wrongWayMonitor.reset();
		postProcessing.resetAnimator();
		playerLapMonitor.reset();
		progressData.reset(false);
		progressData.resetLogicStates();

		// accuDriftSeconds.value = 0;
		accuDriftSeconds.value = DriftBar.MaxSeconds;
		isCurrentLapValid = true;
	}

	/** Restarts the current game */
	@Override
	public void restartGame () {
		realRestart();
		GameEvents.logicEvent.trigger(this, GameLogicEvent.Type.GameRestart);
	}

	/** Restart and completely resets the game, removing any previous recording and playing replays FIXME not sure this is still
	 * useful, maybe for debugging purposes.. */
	@Override
	public void resetGame () {
		realRestart();

		// also remove all replays so far
		lapManager.removeAllReplays();

		GameEvents.logicEvent.trigger(this, GameLogicEvent.Type.GameReset);
	}

	protected void setAccuDriftSeconds (float value) {
		accuDriftSeconds.value = value;
	}

	@Override
	public void pauseGame () {
		paused = true;
		postProcessing.gamePause(500);
		if (gameInput.isTimeDilating()) {
			getTimeDilationTimer().stop();
			// endTimeDilation();
		}
	}

	@Override
	public void resumeGame () {
		paused = false;
		postProcessing.gameResume(500);
		if (gameInput.isTimeDilating()) {
			getTimeDilationTimer().resume();
			gameInput.ensureConsistenceAfterResume();
		}
	}

	@Override
	public void quitGame () {
		quitPending = true;
		Gdx.app.log("CommonLogic", "QUIT request scheduled");
	}

	@Override
	public boolean isQuitPending () {
		return quitPending;
	}

	@Override
	public boolean isPaused () {
		return paused;
	}

	@Override
	public boolean isTimeDilationAvailable () {
		return accuDriftSeconds.value > 0;
	}

	@Override
	public void showMessage (String message, float durationSecs, Message.Type type, Position position, Size size) {
		messager.show(message, durationSecs, type, position, size);
	};

	@Override
	public void tick () {
		if (quitPending) {
			_doQuit();
		} else {
			// compute the next-frame time multiplier
			URacer.timeMultiplier = getTimeModulator().getTime();
			if (!paused) {
				gameInput.update();
				handleExtraInput();
			}
		}
	}

	@Override
	public void tickCompleted () {
		if (!quitPending && !paused) {
			updateLogic();
		}
	}

	//
	// private impl
	//

	private void _doQuit () {
		if (quitPending && !quitScheduled) {
			Gdx.app.log("CommonLogic", "Scheduling QUIT request");
			quitScheduled = true;
			GameEvents.logicEvent.trigger(this, GameLogicEvent.Type.GameQuit);
			doQuit();
		}
	}

	private void updateLogic () {
		updateTrackStates();
		updateGhostMonitors();

		if (hasPlayer()) {
			updatePlayerMonitors();

			// blink CarHighlighter on out of track (keeps calling, returns earlier if busy)
			if (playerCar.isOutOfTrack()) {
				playerTasks.hudPlayer.highlightOutOfTrack();
			}

			// ends time dilation if no more seconds available
			if (accuDriftSeconds.value == 0 && gameInput.isTimeDilating()) {
				endTimeDilation();
				Gdx.app.log("CommonLogic", "Requesting time modulation to finish");
			}

			if (!playerLapMonitor.isWarmUp()) {
				// Time dilationTime = getTimeDilationTimer();
				// Gdx.app.log("", "stopped=" + dilationTime.isStopped());
				updateDriftSeconds();
			}

			updatePlayerDriftBar();
			updateTrackProgress();
		}
	}

	/** Updates cars track states so that GameTrack can be queried */
	private void updateTrackStates () {
		if (hasPlayer()) {
			gameTrack.updateTrackState(playerCar);
		}

		for (int i = 0; i < ghostCars.length; i++) {
			GhostCar ghost = ghostCars[i];
			if (ghost != null && ghost.hasReplay()) {
				gameTrack.updateTrackState(ghost);
			}
		}
	}

	/** Updates player-monitoring components and raise events accordingly (LapMonitor, WrongWayMonitor) */
	private void updatePlayerMonitors () {
		// triggers wrong way event callback (only begins)
		wrongWayMonitor.update(gameTrack.getTrackRouteConfidence(playerCar));

		// update lap validity
		isCurrentLapValid = isCurrentLapValid && !wrongWayMonitor.isWrongWay();

		if (isCurrentLapValid) {
			// triggers lap event callbacks
			playerLapMonitor.update(playerCar);
		} else {
			// keeps signaling the error with a CarHighlighter
			// blink CarHighlighter on wrong way or too slow (keeps calling, returns earlier if busy)
			playerTasks.hudPlayer.highlightWrongWay();

			// inhibits lap monitor to raise events
			playerLapMonitor.reset();
		}
	}

	/** Updates ghost lap monitors */
	private void updateGhostMonitors () {
		for (int i = 0; i < ghostCars.length; i++) {
			GhostCar ghost = ghostCars[i];
			if (ghost.hasReplay()) {
				ghostLapMonitor[i].update(ghost);
			}
		}
	}

	/** Updates player hud drift bar */
	private void updatePlayerDriftBar () {
		DriftBar driftBar = playerTasks.hudPlayer.driftBar;
		driftBar.setDriftStrength(playerCar.driftState.driftStrength);
		driftBar.setSeconds(accuDriftSeconds.value);
	}

	private void updateDriftSeconds () {
		if (Config.Debug.InfiniteDilationTime) {
			accuDriftSeconds.value = DriftBar.MaxSeconds;
		} else {
			// earn game-time seconds by drifting
			if (playerCar.driftState.isDrifting) {
				accuDriftSeconds.value += Config.Physics.Dt + Config.Physics.Dt * playerCar.driftState.driftStrength;
			}

			Time dilationTime = getTimeDilationTimer();
			Time outOfTrackTime = getOutOfTrackTimer();

			// lose wall-clock seconds while in time dilation
			if (!dilationTime.isStopped()) {
				float val = dilationTime.elapsed().lastAbsSeconds;
				accuDriftSeconds.value -= val;
			}

			// lose wall-clock seconds while out of track
			if (!outOfTrackTime.isStopped()) {
				accuDriftSeconds.value -= outOfTrackTime.elapsed().lastAbsSeconds;
			}

			// lose wall-clock seconds on collision
			accuDriftSeconds.value -= Config.Physics.Dt * 5 * getCollisionFactor();

			accuDriftSeconds.value = MathUtils.clamp(accuDriftSeconds.value, 0, DriftBar.MaxSeconds);
		}
	}

	/** Updates player hud track progress */
	private void updateTrackProgress () {
		GhostCar nextTarget = getNextTarget();

		// update progress data
		progressData.update(playerLapMonitor.isWarmUp(), isCurrentLapValid, gameTrack, playerCar, nextTarget);

		// update progress visuals, messages
		playerTasks.hudPlayer.trackProgress.update(gameTrack, playerCar, nextTarget);
	}
}
