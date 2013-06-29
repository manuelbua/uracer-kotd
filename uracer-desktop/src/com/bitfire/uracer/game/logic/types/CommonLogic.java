
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
import com.bitfire.uracer.game.Time.Reference;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.events.GameLogicEvent;
import com.bitfire.uracer.game.logic.gametasks.GameTasksManager;
import com.bitfire.uracer.game.logic.gametasks.Messager;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.player.DriftBar;
import com.bitfire.uracer.game.logic.helpers.CarFactory;
import com.bitfire.uracer.game.logic.helpers.GameTrack;
import com.bitfire.uracer.game.logic.helpers.PlayerGameTasks;
import com.bitfire.uracer.game.logic.post.PostProcessing;
import com.bitfire.uracer.game.logic.replaying.LapManager;
import com.bitfire.uracer.game.logic.replaying.Replay;
import com.bitfire.uracer.game.logic.replaying.ReplayManager;
import com.bitfire.uracer.game.logic.types.helpers.GhostLapCompletionMonitor;
import com.bitfire.uracer.game.logic.types.helpers.PlayerLapCompletionMonitor;
import com.bitfire.uracer.game.logic.types.helpers.WrongWayMonitor;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.rendering.GameWorldRenderer;
import com.bitfire.uracer.game.screens.GameScreensFactory.ScreenType;
import com.bitfire.uracer.game.tween.GameTweener;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.screen.TransitionFactory.TransitionType;
import com.bitfire.uracer.utils.BoxedFloat;

public abstract class CommonLogic implements GameLogic, GameLogicObserver {
	// input
	protected Input inputSystem = null;
	protected GameInput gameInput = null;
	protected boolean quitPending = false, quitScheduled = false;

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
	protected boolean isCollisionPenalty;
	private GhostCar nextTarget = null;

	// lap / replays
	protected LapManager lapManager = null;
	protected PlayerLapCompletionMonitor playerLapMonitor = null;
	private PlayerLapCompletionMonitor[] ghostLapMonitor = new GhostLapCompletionMonitor[ReplayManager.MaxReplays];

	// tasks
	protected GameTasksManager gameTasksManager = null;
	protected PlayerGameTasks playerTasks = null;
	protected Messager messager = null;

	private BoxedFloat accuDriftSeconds = new BoxedFloat(0);

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
		playerTasks.createTasks(lapManager);

		// create ghost cars
		for (int i = 0; i < ReplayManager.MaxReplays; i++) {
			ghostCars[i] = CarFactory.createGhost(i, gameWorld);
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

		// create game input
		gameInput = new GameInput(this, inputSystem);
	}

	public boolean hasPlayer () {
		return playerCar != null;
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

		for (int i = 0; i < ReplayManager.MaxReplays; i++) {
			if (ghostCars[i] != null) {
				ghostCars[i].dispose();
			}
		}

		eventHandlers.unregisterGhostEvents();
		eventHandlers.unregisterRenderEvents();

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

		gameTrack.resetTrackState(playerCar);

		GameEvents.logicEvent.player = playerCar;
		GameEvents.logicEvent.trigger(this, GameLogicEvent.Type.PlayerAdded);

		Gdx.app.log("GameLogic", "Game tasks created and configured");

		eventHandlers.registerPlayerEvents();
		eventHandlers.registerPlayerMonitorEvents();
		Gdx.app.log("GameLogic", "Registered player-related events");

		postProcessing.setPlayer(playerCar);
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
			GameEvents.logicEvent.trigger(this, GameLogicEvent.Type.PlayerRemoved);
		}

		gameWorld.setPlayer(null);
		gameWorldRenderer.setRenderPlayerHeadlights(false);
		wrongWayMonitor.reset();
		lapManager.reset(true);
		playerLapMonitor.reset();
		postProcessing.setPlayer(null);
	}

	private void realRestart (boolean raiseEvent) {
		resetPlayer(gameWorld, playerCar);
		resetAllGhosts();
		endTimeDilation();

		getOutOfTrackTimer().reset();
		lapManager.abortRecording(true);
		wrongWayMonitor.reset();
		postProcessing.resetAnimator();
		playerLapMonitor.reset();

		accuDriftSeconds.value = 0;
		isCurrentLapValid = true;
		isCollisionPenalty = false;

		if (raiseEvent) {
			GameEvents.logicEvent.trigger(this, GameLogicEvent.Type.GameRestart);
		}
	}

	/** Restarts the current game */
	@Override
	public void restartGame () {
		realRestart(true);
	}

	/** Restart and completely resets the game, removing any previous recording and playing replays */
	@Override
	public void resetGame () {
		realRestart(false);

		// clean everything
		lapManager.removeAllReplays();
		lapManager.reset(true);

		GameEvents.logicEvent.trigger(this, GameLogicEvent.Type.GameReset);
	}

	@Override
	public void quitGame () {
		quitPending = true;
	}

	@Override
	public boolean isQuitPending () {
		return quitPending;
	}

	@Override
	public boolean isTimeDilationAvailable () {
		return accuDriftSeconds.value > 0;
	}

	@Override
	public GhostCar getNextTarget () {
		return nextTarget;
	}

	@Override
	public void tick () {
		if (quitPending) {
			doQuit();
		} else {
			// compute the next-frame time multiplier
			URacer.timeMultiplier = getTimeModulator().getTime();
			gameInput.update();
			handleExtraInput();
		}
	}

	@Override
	public void tickCompleted () {
		if (!quitPending) {
			updateLogic();
		}
	}

	//
	// private impl
	//

	private void doQuit () {
		if (quitPending && !quitScheduled) {
			quitScheduled = true;

			GameEvents.logicEvent.trigger(this, GameLogicEvent.Type.GameQuit);

			lapManager.abortRecording(false);

			URacer.Screens.setScreen(ScreenType.MainScreen, TransitionType.Fader, 500);
			// URacer.Screens.setScreen( ScreenType.ExitScreen, TransitionType.Fader, 500 );

			getTimeModulator().reset();
			URacer.Game.resetTimeModFactor();
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

			updatePlayerDriftBar();
			updateTrackProgress();
		}
	}

	/** Updates cars track states so that GameTrack can be queried */
	private void updateTrackStates () {
		if (hasPlayer()) {
			gameTrack.updateTrackState(playerCar);
		}

		for (int i = 0; i < ReplayManager.MaxReplays; i++) {
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
		for (int i = 0; i < ReplayManager.MaxReplays; i++) {
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
				accuDriftSeconds.value -= dilationTime.elapsed(Reference.LastAbsoluteSeconds) * 2;
			}

			// lose wall-clock seconds while out of track
			if (!outOfTrackTime.isStopped()) {
				accuDriftSeconds.value -= outOfTrackTime.elapsed(Reference.LastAbsoluteSeconds);
			}

			// lose wall-clock seconds on collision
			accuDriftSeconds.value -= Config.Physics.Dt * 5 * getCollisionFactor();
		}

		accuDriftSeconds.value = MathUtils.clamp(accuDriftSeconds.value, 0, DriftBar.MaxSeconds);
		driftBar.setSeconds(accuDriftSeconds.value);
	}

	/** Updates player hud track progress */
	private void updateTrackProgress () {
		playerTasks.hudPlayer.trackProgress
			.update(playerLapMonitor.isWarmUp(), isCurrentLapValid, gameTrack, playerCar, nextTarget);
	}

	private void configurePlayer (GameWorld world, Input inputSystem, PlayerCar player) {
		player.setInputSystem(inputSystem);
		player.setFrictionMap(Art.frictionMapDesert);
		player.setWorldPosMt(world.playerStart.position, world.playerStart.orientation);
		player.resetPhysics();
	}

	private void resetPlayer (GameWorld world, Car playerCar) {
		if (playerCar != null) {
			playerCar.resetPhysics();
			playerCar.resetDistanceAndSpeed(true, true);
			playerCar.setWorldPosMt(world.playerStart.position, world.playerStart.orientation);
			gameTrack.resetTrackState(playerCar);
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
		ghostLapMonitor[ghost].reset();
	}

	private void resetGhost (int handle) {
		GhostCar ghost = ghostCars[handle];
		if (ghost != null) {
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

		int ghostIndex = 0;
		for (Replay r : lapManager.getReplays()) {
			if (r.isValid()) {
				setGhostReplay(ghostIndex, r);
				if (lapManager.getBestReplay() == r) {
					nextTarget = ghostCars[ghostIndex];
					playerTasks.hudPlayer.highlightNextTarget(nextTarget);
				}

				ghostIndex++;
			} else {
				setGhostReplay(ghostIndex, null);
			}
		}
	}
}
