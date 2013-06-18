
package com.bitfire.uracer.game.logic.types;

import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.equations.Quad;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.game.DebugHelper;
import com.bitfire.uracer.game.GameInput;
import com.bitfire.uracer.game.GameLogic;
import com.bitfire.uracer.game.Time;
import com.bitfire.uracer.game.Time.Reference;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.CarPreset;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.events.CarEvent;
import com.bitfire.uracer.game.logic.GameTasksManager;
import com.bitfire.uracer.game.logic.gametasks.Messager;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.HudPlayer.EndDriftType;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.player.DriftBar;
import com.bitfire.uracer.game.logic.helpers.CarFactory;
import com.bitfire.uracer.game.logic.helpers.GameTrack;
import com.bitfire.uracer.game.logic.helpers.PlayerGameTasks;
import com.bitfire.uracer.game.logic.post.PostProcessing;
import com.bitfire.uracer.game.logic.replaying.LapManager;
import com.bitfire.uracer.game.logic.replaying.Replay;
import com.bitfire.uracer.game.logic.replaying.ReplayManager;
import com.bitfire.uracer.game.logic.replaying.ReplayRecorder.RecorderError;
import com.bitfire.uracer.game.logic.types.helpers.GhostLapCompletionMonitor;
import com.bitfire.uracer.game.logic.types.helpers.PlayerLapCompletionMonitor;
import com.bitfire.uracer.game.logic.types.helpers.TimeModulator;
import com.bitfire.uracer.game.logic.types.helpers.WrongWayMonitor;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.rendering.GameWorldRenderer;
import com.bitfire.uracer.game.screens.GameScreensFactory.ScreenType;
import com.bitfire.uracer.game.tween.GameTweener;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.screen.TransitionFactory.TransitionType;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.BoxedFloat;
import com.bitfire.uracer.utils.BoxedFloatAccessor;
import com.bitfire.uracer.utils.InterpolatedFloat;
import com.bitfire.uracer.utils.NumberString;

public abstract class CommonLogic implements GameLogic {

	protected void beforeRender () {
		// request camera updates from callbacks
		float zoom = updateCameraZoom(URacer.Game.getTimeModFactor());
		updateCameraPosition(cameraPos);

		// apply camera updates
		gameWorldRenderer.setCameraZoom(zoom);
		gameWorldRenderer.setCameraPosition(cameraPos);
		gameWorldRenderer.updateCamera();

		// sync post-processing animators
		postProcessing.onBeforeRender(zoom, playerLapMonitor.getWarmUpCompletion());

		// game tweener step
		GameTweener.update();
	}

	protected float updateCameraZoom (float timeModFactor) {
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

	protected abstract void updateCameraPosition (Vector2 positionPx);

	private TweenCallback penaltyFinished = new TweenCallback() {
		@Override
		public void onEvent (int type, BaseTween<?> source) {
			switch (type) {
			case COMPLETE:
				isPenalty = false;
			}
		}
	};

	protected void collision (CarEvent.Data eventData) {
		// invalidate time modulation
		if (gameInput.isTimeDilating()) {
			endTimeDilation();
		}

		postProcessing.alert(4000);

		if (!isPenalty) {
			isPenalty = true;
			GameTweener.stop(accuDriftSeconds);
			GameTweener.start(Timeline.createSequence()
				.push(Tween.to(accuDriftSeconds, BoxedFloatAccessor.VALUE, 500).target(0).ease(Quad.INOUT))
				.setCallback(penaltyFinished));
			playerTasks.hudPlayer.highlightCollision();
		}
	}

	protected void physicsForcesReady (CarEvent.Data eventData) {
		RecorderError recerror = lapManager.record(eventData.forces);
		if (recerror == RecorderError.ReplayMemoryLimitReached) {
			Gdx.app.log("CommonLogic", "Player too slow, recording aborted.");
			playerError("Too slow!");
		}
		// RecordingNotEnabled
		// NoError
	}

	protected void ghostReplayEnded (GhostCar ghost) {
	}

	protected void ghostLapStarted (GhostCar ghost) {
	}

	protected void ghostLapCompleted (GhostCar ghost) {
	}

	protected void ghostFadingOut (GhostCar ghost) {
		if (hasPlayer() && ghost != null && ghost == nextTarget) {
			playerTasks.hudPlayer.unHighlightNextTarget();
		}
	}

	protected void playerLapStarted () {
	}

	protected void playerLapCompleted () {
	}

	protected void warmUpStarted () {
	}

	protected void warmUpCompleted () {
	}

	protected void driftBegins (PlayerCar player) {
		playerTasks.hudPlayer.beginDrift();
	}

	protected void driftEnds (PlayerCar player) {
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
	}

	protected void wrongWayBegins () {
		playerTasks.hudPlayer.wrongWay.fadeIn();
		playerError("Invalid lap");
	}

	protected void wrongWayEnds () {
	}

	protected void outOfTrack () {
		outOfTrackTime.start();
		playerTasks.hudPlayer.driftBar.showSecondsLabel();
	}

	protected void backInTrack () {
		// updateDriftBar();
		outOfTrackTime.reset();
		playerTasks.hudPlayer.driftBar.hideSecondsLabel();
	}

	// input
	protected Input inputSystem = null;
	protected GameInput gameInput = null;
	protected boolean quitPending = false, quitScheduled = false;

	// world
	protected GameWorld gameWorld = null;
	protected GameTrack gameTrack = null;

	// rendering
	protected GameWorldRenderer gameWorldRenderer = null;
	protected PostProcessing postProcessing = null;
	private Vector2 cameraPos = new Vector2();
	private float prevZoom = GameWorldRenderer.MinCameraZoom + GameWorldRenderer.ZoomWindow;

	// player
	protected final EventHandlers eventHandlers;
	protected final UserProfile userProfile;
	protected PlayerCar playerCar = null;
	protected GhostCar[] ghostCars = new GhostCar[ReplayManager.MaxReplays];
	private WrongWayMonitor wrongWayMonitor;
	protected boolean isCurrentLapValid = true;
	protected boolean isPenalty;
	private GhostCar nextTarget = null;
	private Time dilationTime = new Time();
	private Time outOfTrackTime = new Time();
	private InterpolatedFloat driftStrength = new InterpolatedFloat();

	// lap / replays
	protected LapManager lapManager = null;
	private PlayerLapCompletionMonitor playerLapMonitor = null;
	private PlayerLapCompletionMonitor[] ghostLapMonitor = new GhostLapCompletionMonitor[ReplayManager.MaxReplays];

	// tasks
	protected GameTasksManager gameTasksManager = null;
	protected PlayerGameTasks playerTasks = null;
	protected Messager messager = null;

	// time modulation logic
	private TimeModulator timeMod = null;

	private BoxedFloat accuDriftSeconds = new BoxedFloat(0);;

	public CommonLogic (UserProfile userProfile, GameWorld gameWorld, GameRenderer gameRenderer) {
		this.userProfile = userProfile;
		this.gameWorld = gameWorld;
		this.gameWorldRenderer = gameRenderer.getWorldRenderer();
		this.inputSystem = URacer.Game.getInputSystem();
		this.gameTrack = gameWorld.getGameTrack();
		this.messager = new Messager();
		this.eventHandlers = new EventHandlers(this);

		timeMod = new TimeModulator();
		lapManager = new LapManager(gameWorld.getLevelId());

		// post-processing
		postProcessing = gameRenderer.getPostProcessing();

		// create both game and player tasks
		gameTasksManager = new GameTasksManager(gameWorld);
		gameTasksManager.add(messager);
		playerTasks = new PlayerGameTasks(userProfile, gameTasksManager);
		playerTasks.createTasks(lapManager);

		for (int i = 0; i < ReplayManager.MaxReplays; i++) {
			ghostCars[i] = CarFactory.createGhost(i, gameWorld, CarPreset.Type.L1_GoblinOrange);
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

		playerTasks.playerAdded(playerCar);
		Gdx.app.log("GameLogic", "Game tasks created and configured");

		eventHandlers.registerPlayerEvents();
		eventHandlers.registerPlayerMonitorEvents();
		Gdx.app.log("GameLogic", "Registered player-related events");

		postProcessing.setPlayer(playerCar);
		gameWorld.setPlayer(playerCar);
		gameWorldRenderer.setRenderPlayerHeadlights(gameWorld.isNightMode());
		gameWorldRenderer.showDebugGameTrack(Config.Debug.RenderTrackSectors);
		gameWorldRenderer.setGameTrackDebugCar(playerCar);

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
			eventHandlers.unregisterPlayerMonitorEvents();
			playerTasks.playerRemoved();
			playerCar.dispose();
		}

		playerCar = null;
		gameWorld.setPlayer(null);
		gameWorldRenderer.setRenderPlayerHeadlights(false);
		wrongWayMonitor.reset();
		lapManager.reset(true);
		playerLapMonitor.reset();
		postProcessing.setPlayer(null);
		driftStrength.reset(0, true);

		if (Config.Debug.UseDebugHelper) {
			DebugHelper.setPlayer(null);
		}
	}

	/** Restarts the current game */
	@Override
	public void restartGame () {
		resetPlayer(gameWorld, playerCar);
		resetAllGhosts();
		endTimeDilation();

		outOfTrackTime.reset();
		lapManager.abortRecording(true);
		gameTasksManager.raiseRestart();
		wrongWayMonitor.reset();
		postProcessing.resetAnimator();
		playerLapMonitor.reset();

		accuDriftSeconds.value = 0;
		isCurrentLapValid = true;
		isPenalty = false;
	}

	/** Restart and completely resets the game, removing any previous recording and playing replays */
	@Override
	public void resetGame () {
		restartGame();

		// clean everything
		lapManager.removeAllReplays();
		lapManager.reset(true);
		gameTasksManager.raiseReset();
	}

	@Override
	public void quitGame () {
		quitPending = true;
	}

	@Override
	public boolean isQuitPending () {
		return quitPending;
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
		// reset it, endTimeDilation can be called out of GameInput as well
		gameInput.resetTimeDilating();
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
		if (quitPending) {
			doQuit();
		} else {
			// compute the next-frame time multiplier
			URacer.timeMultiplier = timeMod.getTime();
			gameInput.update();
			dbgInput();
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

			lapManager.abortRecording(false);
			gameTasksManager.sound.stop();

			URacer.Screens.setScreen(ScreenType.MainScreen, TransitionType.Fader, 500);
			// URacer.Screens.setScreen( ScreenType.ExitScreen, TransitionType.Fader, 500 );

			timeMod.reset();
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

	/** Invalidates the current lap and show an error */
	private void playerError (String message) {
		isCurrentLapValid = false;
		lapManager.abortRecording(true);
		playerTasks.hudLapInfo.setInvalid(message);
		playerTasks.hudLapInfo.toColor(1, 0, 0);
		postProcessing.alertBegins(500);
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
			}
		}
	}

	private void dbgInput () {
		if (inputSystem.isPressed(Keys.O)) {
			removePlayer();
			restartGame();
			restartAllReplays();
		} else if (inputSystem.isPressed(Keys.P)) {
			addPlayer();
			restartGame();
		} else if (inputSystem.isPressed(Keys.W)) {
			Config.Debug.RenderBox2DWorldWireframe = !Config.Debug.RenderBox2DWorldWireframe;
		} else if (inputSystem.isPressed(Keys.B)) {
			Config.Debug.Render3DBoundingBoxes = !Config.Debug.Render3DBoundingBoxes;
		} else if (inputSystem.isPressed(Keys.TAB)) {
			Config.Debug.RenderTrackSectors = !Config.Debug.RenderTrackSectors;
			gameWorldRenderer.showDebugGameTrack(Config.Debug.RenderTrackSectors);
			gameWorldRenderer.setGameTrackDebugCar(playerCar);
		}
		// else if (inputSystem.isPressed(Keys.Z)) {
		// // start recording
		// playerCar.resetDistanceAndSpeed(true, true);
		// resetAllGhosts();
		// lapManager.abortRecording(true);
		// lapManager.startRecording(playerCar, userProfile);
		// Gdx.app.log("GameLogic", "Recording...");
		// }
		// else if (inputSystem.isPressed(Keys.X)) {
		// // stop recording and play
		// Replay userRec = lapManager.stopRecording();
		// playerCar.resetPhysics();
		// playerCar.resetDistanceAndSpeed(true, true);
		// if (userRec != null) {
		// CarUtils.dumpSpeedInfo("Player", playerCar, userRec.getTrackTime());
		// userRec.saveLocal(messager);
		// setGhostReplay(0, userRec);
		// }
		// }
		else if (inputSystem.isPressed(Keys.L)) {
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
}
