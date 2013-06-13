
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
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.GameInput;
import com.bitfire.uracer.game.GameLogic;
import com.bitfire.uracer.game.Time;
import com.bitfire.uracer.game.Time.Reference;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.CarPreset;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.events.CarEvent;
import com.bitfire.uracer.game.events.GameRendererEvent;
import com.bitfire.uracer.game.events.GameRendererEvent.Order;
import com.bitfire.uracer.game.events.GameRendererEvent.Type;
import com.bitfire.uracer.game.events.GhostCarEvent;
import com.bitfire.uracer.game.events.PlayerDriftStateEvent;
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
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.screen.TransitionFactory.TransitionType;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.BoxedFloat;
import com.bitfire.uracer.utils.BoxedFloatAccessor;
import com.bitfire.uracer.utils.CarUtils;
import com.bitfire.uracer.utils.InterpolatedFloat;
import com.bitfire.uracer.utils.NumberString;

public abstract class CommonLogic implements GameLogic {

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

	protected void newReplay (Replay replay) {
	}

	protected void discardedReplay () {
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
	protected GameInput gameInput = null;

	// world
	protected GameWorld gameWorld = null;
	protected GameTrack gameTrack = null;

	// rendering
	protected GameWorldRenderer gameWorldRenderer = null;
	protected PostProcessing postProcessing = null;
	private Vector2 cameraPos = new Vector2();
	private float prevZoom = GameWorldRenderer.MinCameraZoom + GameWorldRenderer.ZoomWindow;

	// player
	protected final EventHandlers eventHandlers = new EventHandlers();
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
	private LapCompletionMonitor lapMonitor = null;

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

		timeMod = new TimeModulator();
		lapManager = new LapManager(userProfile, gameWorld.getLevelId());

		// post-processing
		postProcessing = gameRenderer.getPostProcessing();

		// create both game and player tasks
		gameTasksManager = new GameTasksManager(gameWorld);
		gameTasksManager.add(messager);
		playerTasks = new PlayerGameTasks(userProfile, gameTasksManager);
		playerTasks.createTasks(lapManager);

		for (int i = 0; i < ReplayManager.MaxReplays; i++) {
			ghostCars[i] = CarFactory.createGhost(i, gameWorld, CarPreset.Type.L1_GoblinOrange);
		}
		gameWorld.setGhostCars(ghostCars);

		// register events
		eventHandlers.registerGhostEvents();
		eventHandlers.registerRenderEvents();

		// create monitors and setup listeners
		wrongWayMonitor = new WrongWayMonitor(eventHandlers);
		lapMonitor = new LapCompletionMonitor(eventHandlers, gameTrack);

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

		playerTasks.playerAdded(playerCar);
		Gdx.app.log("GameLogic", "Game tasks created and configured");

		eventHandlers.registerPlayerEvents();
		Gdx.app.log("GameLogic", "Registered player-related events");

		postProcessing.setPlayer(playerCar);
		gameWorld.setPlayer(playerCar);
		gameWorldRenderer.setRenderPlayerHeadlights(gameWorld.isNightMode());
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
			playerTasks.playerRemoved();
			playerCar.dispose();
		}

		playerCar = null;
		gameWorld.setPlayer(null);
		gameWorldRenderer.setRenderPlayerHeadlights(false);
		wrongWayMonitor.reset();
		lapManager.reset(true);
		lapMonitor.reset(null);
		postProcessing.setPlayer(null);
		driftStrength.reset(0, true);

		restartLogic();
		restartAllReplays();

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
		// compute the next-frame time multiplier
		URacer.timeMultiplier = timeMod.getTime();
		gameInput.update();
		dbgInput();
	}

	@Override
	public void tickCompleted () {
		updateLogic();
	}

	//
	// private impl
	//

	private void updateLogic () {

		updateTrackStates();

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

		isCurrentLapValid = isCurrentLapValid && !wrongWayMonitor.isWrongWay();

		if (isCurrentLapValid) {
			// triggers lap event callbacks
			lapMonitor.update();
		} else {
			// blink CarHighlighter on wrong way or too slow (keeps calling, returns earlier if busy)
			playerTasks.hudPlayer.highlightWrongWay();

			// inhibits lap monitor to raise events
			lapMonitor.reset();
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
		playerTasks.hudPlayer.trackProgress.update(lapMonitor.isWarmUp(), isCurrentLapValid, gameTrack, playerCar, nextTarget);
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
		lapManager.abortRecording();
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

	/** Restarts the game */
	private void restartLogic () {
		gameTrack.clearTrackStates();
		resetPlayer(gameWorld, playerCar);
		resetAllGhosts();
		endTimeDilation();

		outOfTrackTime.reset();
		lapManager.abortRecording();
		gameTasksManager.raiseRestart();
		wrongWayMonitor.reset();
		postProcessing.resetAnimator();
		lapMonitor.reset(playerCar);

		accuDriftSeconds.value = 0;
		isCurrentLapValid = true;
		isPenalty = false;
	}

	/** Resets the game, any in-memory recorded replay will be discarded */
	private void resetLogic () {
		restartLogic();

		// clean everything
		lapManager.removeAllReplays();
		lapManager.reset(true);
		gameTasksManager.raiseReset();
	}

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
			lapManager.startRecording(playerCar);
			Gdx.app.log("GameLogic", "Recording...");

		} else if (inputSystem.isPressed(Keys.X)) {

			// stop recording and play
			Replay userRec = lapManager.stopRecording();
			playerCar.resetPhysics();
			playerCar.resetDistanceAndSpeed(true, true);
			if (userRec != null) {
				CarUtils.dumpSpeedInfo("Player", playerCar, userRec.getTrackTime());
				userRec.saveLocal(messager);
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
			lapStarted();
		}

		@Override
		public void onLapCompleted () {
			Gdx.app.log("CommonLogic", "Lap Completed");
			if (lapManager.isRecording()) {
				Replay last = lapManager.stopRecording();
				if (last != null) {
					// FIXME, change name?
					// FIXME, should also pass more information? such as replay classification (was this replay better than all? than
					// what?)
					newReplay(last);
				} else {
					// discarded if worse than the worst
					discardedReplay();
				}
			}

			playerCar.resetDistanceAndSpeed(true, false);
			lapCompleted();
		}

		@Override
		public void onWrongWayBegins () {
			playerTasks.hudPlayer.wrongWay.fadeIn();
			playerError("Invalid lap");
		}

		@Override
		public void onWrongWayEnds () {
		}

		//
		// renderer listener
		//
		private GameRendererEvent.Listener rendererListener = new GameRendererEvent.Listener() {
			@SuppressWarnings("incomplete-switch")
			@Override
			public void handle (Object source, Type type, Order order) {
				switch (type) {
				case BeforeRender:
					// request camera updates from callbacks
					float zoom = updateCameraZoom(URacer.Game.getTimeModFactor());
					updateCameraPosition(cameraPos);

					// apply camera updates
					gameWorldRenderer.setCameraZoom(zoom);
					gameWorldRenderer.setCameraPosition(cameraPos);
					gameWorldRenderer.updateCamera();

					// sync post-processing animators
					postProcessing.onBeforeRender(zoom, lapMonitor.getWarmUpCompletion());

					// game tweener step
					GameTweener.update();
					break;
				}
			}
		};

		//
		// drift state listener
		//

		private PlayerDriftStateEvent.Listener driftStateListener = new PlayerDriftStateEvent.Listener() {
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

		//
		// car listener
		//

		private CarEvent.Listener playerCarListener = new CarEvent.Listener() {
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
					if (gameInput.isTimeDilating()) {
						endTimeDilation();
					}

					postProcessing.alert(0.75f, 4000);

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
						playerError("Too slow!");
					}
					// RecordingNotEnabled
					// NoError

					break;
				}
			}
		};

		//
		// ghost car-specific listener
		//

		private GhostCarEvent.Listener ghostListener = new GhostCarEvent.Listener() {
			@Override
			public void handle (Object source, GhostCarEvent.Type type, GhostCarEvent.Order order) {
				switch (type) {
				case onGhostFadingOut:
					if (hasPlayer() && source == nextTarget) {
						playerTasks.hudPlayer.unHighlightNextTarget();
					}
					break;
				case ReplayEnded:
					GhostCar ghost = (GhostCar)source;
					if (!hasPlayer()) {
						ghost.restartReplay();
					} else {
						ghost.removeReplay();
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
			GameEvents.ghostCars.addListener(ghostListener, GhostCarEvent.Type.ReplayEnded);
		}

		public void unregisterGhostEvents () {
			GameEvents.ghostCars.removeListener(ghostListener, GhostCarEvent.Type.onGhostFadingOut);
			GameEvents.ghostCars.removeListener(ghostListener, GhostCarEvent.Type.ReplayEnded);
		}

		public void registerRenderEvents () {
			GameEvents.gameRenderer.addListener(rendererListener, GameRendererEvent.Type.BeforeRender,
				GameRendererEvent.Order.MINUS_4);
		}

		public void unregisterRenderEvents () {
			GameEvents.gameRenderer.removeListener(rendererListener, GameRendererEvent.Type.BeforeRender,
				GameRendererEvent.Order.MINUS_4);
		}
	}
}
