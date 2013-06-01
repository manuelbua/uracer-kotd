
package com.bitfire.uracer.game.logic.types;

import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.equations.Quad;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.events.CarEvent;
import com.bitfire.uracer.events.PlayerDriftStateEvent;
import com.bitfire.uracer.game.DebugHelper;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.GameLogic;
import com.bitfire.uracer.game.GameplaySettings;
import com.bitfire.uracer.game.Time;
import com.bitfire.uracer.game.Time.Reference;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.CarPreset;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.logic.gametasks.GameTasksManager;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.HudPlayer.EndDriftType;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.player.DriftBar;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.player.TrackProgress.TrackProgressData;
import com.bitfire.uracer.game.logic.helpers.CarFactory;
import com.bitfire.uracer.game.logic.helpers.GameTrack;
import com.bitfire.uracer.game.logic.helpers.PlayerGameTasks;
import com.bitfire.uracer.game.logic.post.PostProcessing;
import com.bitfire.uracer.game.logic.post.PostProcessing.Effects;
import com.bitfire.uracer.game.logic.post.ssao.Ssao;
import com.bitfire.uracer.game.logic.replaying.LapManager;
import com.bitfire.uracer.game.logic.replaying.Replay;
import com.bitfire.uracer.game.logic.replaying.ReplayManager;
import com.bitfire.uracer.game.logic.replaying.ReplayRecorder.RecorderError;
import com.bitfire.uracer.game.logic.types.helpers.GameInput;
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
	protected boolean isWarmUpLap = true;
	protected boolean isWrongWayInWarmUp = false;
	protected boolean isTooSlow = false;
	protected boolean isPenalty;
	private GhostCar nextTarget = null;
	private Time dilationTime = new Time();
	private Time outOfTrackTime = new Time();

	// lap
	protected LapManager lapManager = null;
	private LapCompletionMonitor lapMonitor = null;

	// tasks
	protected GameTasksManager gameTasksManager = null;
	protected PlayerGameTasks playerTasks = null;

	// time modulation logic
	private TimeModulator timeMod = null;

	protected ReplayManager replayManager;
	private BoxedFloat accuDriftSeconds = new BoxedFloat(0);;

	public CommonLogic (UserProfile userProfile, GameWorld gameWorld, GameRenderer gameRenderer) {
		this.userProfile = userProfile;
		this.gameWorld = gameWorld;
		this.gameRenderer = gameRenderer;
		this.gameWorldRenderer = gameRenderer.getWorldRenderer();
		this.inputSystem = URacer.Game.getInputSystem();
		timeMod = new TimeModulator();

		Gdx.app.log("CommonLogic", "Tweening helpers created");

		// post-processing
		postProcessing = new PostProcessing(gameWorld);
		gameRenderer.setEnableNormalDepthMap(postProcessing.requiresNormalDepthMap());
		gameRenderer.setPostProcessor(postProcessing.getPostProcessor());

		if (postProcessing.hasEffect(Effects.Ssao.name)) {
			Ssao ssao = (Ssao)postProcessing.getEffect(Effects.Ssao.name);
			ssao.setNormalDepthMap(gameWorldRenderer.getNormalDepthMap().getColorBufferTexture());
		}

		// main game tasks
		gameTasksManager = new GameTasksManager(gameWorld);
		gameTasksManager.createTasks();

		// player tasks
		playerTasks = new PlayerGameTasks(userProfile, gameTasksManager);

		lapManager = new LapManager(userProfile, gameWorld.getLevelId());
		for (int i = 0; i < ReplayManager.MaxReplays; i++) {
			ghostCars[i] = CarFactory.createGhost(i, gameWorld, CarPreset.Type.L1_GoblinOrange);
		}

		eventHandlers.registerGhostEvents();

		gameWorldRenderer.setGhostCars(ghostCars);
		replayManager = new ReplayManager(userProfile, gameWorld.getLevelId());
		gameTrack = gameWorld.getGameTrack();

		wrongWayMonitor = new WrongWayMonitor(eventHandlers);
		lapMonitor = new LapCompletionMonitor(eventHandlers, gameTrack);

		input = new GameInput(this, inputSystem);
		dilationTime.stop();
		outOfTrackTime.stop();
	}

	@Override
	public void dispose () {
		removePlayer();
		gameTrack.dispose();
		gameTasksManager.dispose();
		postProcessing.dispose();
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
		replayManager.dispose();
	}

	//
	// specific game logic shall also implement these
	//

	// implementers should returns the camera zoom amount
	protected abstract float updateCamera (float timeModFactor);

	protected abstract void newReplay (Replay replay);

	protected abstract void discardedReplay (Replay replay);

	protected abstract void lapStarted ();

	protected abstract void lapCompleted ();

	protected abstract void driftBegins ();

	protected abstract void driftEnds ();

	protected abstract void outOfTrack ();

	protected abstract void backInTrack ();

	protected void wrongWayBegins () {
		postProcessing.alertWrongWayBegins(500);
	}

	protected void wrongWayEnds () {
		postProcessing.alertWrongWayEnds(500);
	}

	//
	// SHARED OPERATIONS (Subclass Sandbox pattern)
	//

	/** Restarts the current game */
	@Override
	public void restartGame () {
		restartLogic();
	}

	/** Restart and completely resets the game, removing any playing replay so far */
	@Override
	public void resetGame () {
		resetLogic();
	}

	/** Request time dilation to begin */
	@Override
	public void startTimeDilation () {
		dilationTime.start();
		timeMod.toDilatedTime();
	}

	/** Request time dilation to end */
	@Override
	public void endTimeDilation () {
		updateDriftBar();
		dilationTime.reset();
		timeMod.toNormalTime();
	}

	/** Sets the player from the specified preset */
	@Override
	public void setPlayer (CarPreset.Type presetType) {
		if (hasPlayer()) {
			Gdx.app.log("GameLogic", "A player already exists.");
			return;
		}

		playerCar = CarFactory.createPlayer(gameWorld, presetType);

		configurePlayer(gameWorld, inputSystem, playerCar);
		Gdx.app.log("GameLogic", "Player configured");

		playerTasks.createTasks(playerCar, lapManager.getLapInfo(), gameRenderer);
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
		gameWorldRenderer.setRenderPlayerHeadlights(false);
		wrongWayMonitor.reset();
		lapMonitor.setCar(null);

		if (Config.Debug.UseDebugHelper) {
			DebugHelper.setPlayer(null);
		}
	}

	public GameWorld getGameWorld () {
		return gameWorld;
	}

	public boolean hasPlayer () {
		return playerCar != null;
	}

	public PlayerCar getPlayer () {
		return playerCar;
	}

	//
	// private implementation
	//

	private void configurePlayer (GameWorld world, Input inputSystem, PlayerCar player) {
		// create player and setup player input system and initial position in
		// the world
		player.setInputSystem(inputSystem);

		// FIXME this is for debug
		player.setFrictionMap(Art.frictionMapDesert);

		player.setWorldPosMt(world.playerStart.position, world.playerStart.orientation);
		player.resetPhysics();
	}

	private void resetPlayer (GameWorld world, Car playerCar) {
		if (playerCar != null) {
			playerCar.resetPhysics();
			playerCar.getTrackState().reset();
			playerCar.resetDistanceAndSpeed(true, true);
			playerCar.setWorldPosMt(world.playerStart.position, world.playerStart.orientation);
		}
	}

	private void resetGhost (int handle) {
		GhostCar ghost = ghostCars[handle];
		if (ghost != null) {
			ghost.getTrackState().reset();
			ghost.resetPhysics();
			ghost.resetDistanceAndSpeed(true, true);
			ghost.removeReplay();
		}
	}

	protected GhostCar getGhost (int handle) {
		return ghostCars[handle];
	}

	private void resetAllGhosts () {
		for (int i = 0; i < ReplayManager.MaxReplays; i++) {
			resetGhost(i);
		}
	}

	private void restartLogic () {
		gameTasksManager.sound.stop();
		resetPlayer(gameWorld, playerCar);
		resetAllGhosts();

		timeMod.reset();
		SysTweener.clear();
		GameTweener.clear();
		lapManager.abortRecording();
		gameTasksManager.restart();

		wrongWayMonitor.reset();
		isCurrentLapValid = true;
		isWarmUpLap = true;
		isWrongWayInWarmUp = false;
		isTooSlow = false;

		postProcessing.resetAnimator();

		// playerTasks.playerEngineSoundFx.start();
		playerTasks.playerDriftSoundFx.start();
		playerTasks.hudLapInfo.toDefaultColor();
		playerTasks.hudLapInfo.setValid(true);
		playerTasks.hudPlayer.getTrackProgressData().reset(false);

		lapMonitor.reset();
		gameTrack.setInitialCarSector(playerCar);
		lapMonitor.setCar(playerCar);

		accuDriftSeconds.value = 0;
		lastDist = 0;
		lastCompletion = 0;
	}

	private void resetLogic () {
		restartLogic();

		// clean everything
		replayManager.reset();
		lapManager.reset();
		gameTasksManager.reset();
	}

	private void checkValidLap () {
		boolean wrongWay = wrongWayMonitor.isWrongWay();
		isCurrentLapValid = !wrongWay && !isTooSlow;

		if ((wrongWay && isWarmUpLap) || isWrongWayInWarmUp) {
			isWrongWayInWarmUp = true;
			isWarmUpLap = true;
			lapMonitor.reset();
		}

		// blink on wrong way (keeps calling, returns earlier if busy)
		if (wrongWay) {
			playerTasks.hudPlayer.highlightWrongWay();
		}

		// blink on out of track (keeps calling, returns earlier if busy)
		if (playerCar.isOutOfTrack()) {
			playerTasks.hudPlayer.highlightOutOfTrack();
		}

		if (isTooSlow) {
			playerTasks.hudPlayer.getTrackProgressData().reset(true);
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

				accuDriftSeconds.value = MathUtils.clamp(accuDriftSeconds.value, 0, DriftBar.MaxSeconds);
			}
		}

		playerTasks.hudPlayer.driftBar.setSeconds(accuDriftSeconds.value);
	}

	//
	// implement interfaces and listeners callbacks
	//

	@Override
	public void tick () {
		input.update();
		dbgInput();
		updateDriftBar();
	}

	private float lastDist, lastCompletion;

	@Override
	public void tickCompleted () {
		if (hasPlayer()) {
			gameTrack.updateCarSector(playerCar);
		}

		for (int i = 0; i < ReplayManager.MaxReplays; i++) {
			if (ghostCars[i] != null && ghostCars[i].hasReplay()) {
				gameTrack.updateCarSector(ghostCars[i]);
			}
		}

		if (hasPlayer()) {
			wrongWayMonitor.update(gameTrack.getTrackRouteConfidence(playerCar));
		}

		lapMonitor.update(isWarmUpLap);

		// determine player's isWrongWay
		if (hasPlayer()) {
			checkValidLap();
		}

		if (accuDriftSeconds.value == 0 && input.isTimeDilating()) {
			endTimeDilation();
			Gdx.app.log("SinglePlayerLogic", "Requesting time modulation to finish");
		}

		if (hasPlayer()) {
			TrackProgressData data = playerTasks.hudPlayer.getTrackProgressData();

			// float ghSpeed = 0;

			// playerTasks.hudPlayer.trackProgress.setPlayerSpeed(playerCar.getInstantSpeed());
			playerTasks.hudPlayer.driftBar.setDriftStrength(playerCar.driftState.driftStrength);

			if (isWarmUpLap) {
				data.reset(true);
				if (isCurrentLapValid) {
					playerTasks.hudPlayer.trackProgress.setMessage("RACE in "
						+ Math.round(gameTrack.getTotalLength() - gameTrack.getTrackDistance(playerCar, 0)) + " mt");
				} else {
					playerTasks.hudPlayer.trackProgress.setMessage("Press \"R\"\nto restart");
				}
			} else {
				if (isCurrentLapValid) {
					playerTasks.hudPlayer.trackProgress.setMessage("");

					// use the last one if the replay is finished
					if (nextTarget != null && nextTarget.hasReplay()) {
						lastDist = gameTrack.getTrackDistance(nextTarget, 0);
						lastCompletion = gameTrack.getTrackCompletion(nextTarget);
						// ghSpeed = nextTarget.getInstantSpeed();
					}

					data.setPlayerDistance(gameTrack.getTrackDistance(playerCar, 0));
					data.setPlayerProgression(gameTrack.getTrackCompletion(playerCar));

					// playerTasks.hudPlayer.trackProgress.setTargetSpeed(ghSpeed);
					data.setTargetDistance(lastDist);
					data.setTargetProgression(lastCompletion);

					// target tracker
					float distMt = gameTrack.getTrackDistance(playerCar, 0) - lastDist;
					float alpha = MathUtils.clamp(Math.abs(distMt) / 50, 0.2f, 1);
					playerTasks.hudPlayer.setNextTargetAlpha(alpha);

				} else {
					playerTasks.hudPlayer.trackProgress.setMessage("Press \"R\"\nto restart");
					data.reset(true);
				}
			}
		}

	}

	private Color ambient = new Color();
	private Color treesAmbient = new Color();

	@Override
	public void beforeRender () {
		URacer.timeMultiplier = timeMod.getTime();
		float zoom = updateCamera(URacer.Game.getTimeModFactor());

		//@off
		ambient.set(
			0.1f,
			0.05f,
			0.15f,
			0.4f + 0.2f * URacer.Game.getTimeModFactor()// + 0.3f * lapMonitor.getWarmUpCompletion()
		);

		treesAmbient.set(
			0.1f,
			0.1f,
			0.15f,
			0.4f + 0.5f * URacer.Game.getTimeModFactor() 
		);
		//@on

		if (gameWorld.isNightMode() && postProcessing.hasEffect(Effects.Crt.name)) {
			gameWorldRenderer.setAmbientColor(0.1f, 0.05f, 0.1f, 0.6f + 0.2f * URacer.Game.getTimeModFactor());
			gameWorldRenderer.setTreesAmbientColor(0.1f, 0.05f, 0.1f, 0.5f + 0.5f * URacer.Game.getTimeModFactor());
		}

		ambient.clamp();
		treesAmbient.clamp();

		gameWorldRenderer.setAmbientColor(ambient);
		gameWorldRenderer.setTreesAmbientColor(treesAmbient);

		// camera/ray handler update
		gameWorldRenderer.updateCamera();

		// Gdx.app.log("CommonLogic", "wuc=" + lapMonitor.getWarmUpCompletion());
		postProcessing.onBeforeRender(zoom, lapMonitor.getWarmUpCompletion());

		// game tweener step
		GameTweener.update();
	}

	@Override
	public void quitGame () {
		lapManager.stopRecording();
		gameTasksManager.sound.stop();

		// quit

		URacer.Screens.setScreen(ScreenType.MainScreen, TransitionType.Fader, 500);
		// URacer.Screens.setScreen( ScreenType.ExitScreen, TransitionType.Fader, 500 );

		timeMod.reset();
		URacer.Game.resetTimeModFactor();
	}

	private Replay userRec = null; // dbg on-demand rec/play via Z/X

	private void dbgInput () {
		if (inputSystem.isPressed(Keys.O)) {
			removePlayer();
		} else if (inputSystem.isPressed(Keys.P)) {
			setPlayer(CarPreset.Type.L1_GoblinOrange);
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

			CarUtils.dumpSpeedInfo("Player", playerCar, lapManager.getLastRecordedReplay().trackTimeSeconds);
			playerCar.resetDistanceAndSpeed(true, true);
			if (userRec != null) {
				userRec.saveLocal(gameTasksManager.messager);
				getGhost(0).setReplay(userRec);
			}

			// Gdx.app.log( "GameLogic", "Player final pos=" +
			// playerCar.getBody().getPosition() );

		} else if (inputSystem.isPressed(Keys.L)) {
			playerCar.resetPhysics();
			playerCar.resetDistanceAndSpeed(true, true);
			lapManager.stopRecording();
			getGhost(0).setReplay(Replay.loadLocal("test-replay"));
		} else if (inputSystem.isPressed(Keys.K)) {
			playerCar.resetPhysics();
			playerCar.resetDistanceAndSpeed(true, true);
			lapManager.stopRecording();
			getGhost(0).setReplay(Replay.loadLocal("test-replay-coll"));
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

	protected void restartAllReplays () {
		Array<Replay> replays = replayManager.getReplays();

		nextTarget = null;
		lastDist = 0;

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

	private TweenCallback penaltyFinished = new TweenCallback() {
		@Override
		public void onEvent (int type, BaseTween<?> source) {
			switch (type) {
			case COMPLETE:
				isPenalty = false;
			}
		}
	};

	protected void collision () {
		if (isPenalty) return;

		isPenalty = true;

		GameTweener.stop(accuDriftSeconds);
		Timeline driftSecondsTimeline = Timeline.createSequence();
		driftSecondsTimeline.push(Tween.to(accuDriftSeconds, BoxedFloatAccessor.VALUE, 500).target(0).ease(Quad.INOUT))
			.setCallback(penaltyFinished);
		GameTweener.start(driftSecondsTimeline);

		playerTasks.hudPlayer.highlightCollision();
	}

	//
	// EVENT HANDLERS
	//

	@Override
	public boolean timeDilationAvailable () {
		return accuDriftSeconds.value > 0;
	}

	// @Override
	// public void carStateEvent (CarState source, CarStateEvent.Type type) {
	// switch (type) {
	// case onTileChanged:
	// if (source.isPlayer) {
	// playerTileChanged(source);
	// } else {
	// ghostTileChanged(source);
	// }
	// break;
	// }
	// }

	private final class EventHandlers implements WrongWayMonitorListener, LapCompletionMonitorListener {

		@Override
		public void onWrongWayBegins () {
			if (lapManager.isRecording()) {
				lapManager.abortRecording();
				lapManager.reset();
			}

			playerTasks.hudPlayer.wrongWay.fadeIn();
			playerTasks.hudLapInfo.toColor(1, 0, 0);
			playerTasks.hudLapInfo.setInvalid("invalid lap");

			wrongWayBegins();
		}

		@Override
		public void onWrongWayEnds () {
			// playerTasks.hudPlayer.wrongWay.fadeOut();
			// playerTasks.hudLapInfo.toColor(1, 1, 0);
			// playerTasks.hudLapInfo.setInvalid("back to start");
			//
			// wrongWayEnds();
		}

		@Override
		public void onLapStarted () {
			Gdx.app.log("CommonLogic", "onLapStarted");

			// lap started, warmup ended
			isWarmUpLap = false;
			playerCar.resetDistanceAndSpeed(true, false);

			// if (!lapManager.isRecording())
			{
				lapManager.stopRecording();
				lapManager.startRecording(playerCar);
			}

			lapStarted();
		}

		@Override
		public void onLapComplete () {
			Gdx.app.log("CommonLogic", "onLapComplete");

			if (!isCurrentLapValid) {
				return;
			}

			if (!playerTasks.hudLapInfo.isValid()) {
				playerTasks.hudLapInfo.setValid(true);
				playerTasks.hudLapInfo.toColor(1, 1, 1);
			}

			// detect and ignore invalid laps
			if (lapManager.isRecording() && lapManager.getLapInfo().getElapsedSeconds() < GameplaySettings.ReplayMinDurationSecs) {
				Gdx.app.log("CommonLogic", "Invalid lap detected, too short (" + lapManager.getLapInfo().getElapsedSeconds()
					+ "sec < " + GameplaySettings.ReplayMinDurationSecs + ")");
				return;
			}

			lapManager.stopRecording();

			// always work on the ReplayManager copy!
			Replay lastRecorded = lapManager.getLastRecordedReplay();
			Replay replay = replayManager.addReplay(lastRecorded);
			if (replay != null) {
				newReplay(replay);
			} else {
				if (lastRecorded != null && lastRecorded.isValid) {
					discardedReplay(lastRecorded);
				}
			}

			playerCar.resetDistanceAndSpeed(true, false);
			lapCompleted();
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
			@Override
			public void handle (Object source, CarEvent.Type type, CarEvent.Order order) {
				CarEvent.Data eventData = GameEvents.playerCar.data;

				switch (type) {
				case onCollision:

					// invalidate drifting
					if (playerCar.driftState.isDrifting) {
						playerCar.driftState.invalidateByCollision();
					}

					// invalidate time modulation
					if (input.isTimeDilating()) {
						endTimeDilation();
					}

					postProcessing.alertCollision(0.75f, 4000);

					collision();
					break;
				case onOutOfTrack:
					outOfTrackTime.start();
					outOfTrack();
					break;
				case onBackInTrack:
					updateDriftBar();
					outOfTrackTime.reset();
					backInTrack();
					break;
				case onComputeForces:
					if (lapManager.record(eventData.forces) == RecorderError.ReplayMemoryLimitReached) {
						Gdx.app.log("CommonLogic", "Player too slow, recording aborted.");
						isTooSlow = true;
						lapManager.abortRecording();
						playerTasks.hudLapInfo.setInvalid("Too slow!");
						playerTasks.hudLapInfo.toColor(1, 0, 0);
					}
					break;
				}
			}
		};

		CarEvent.Listener ghostListener = new CarEvent.Listener() {
			@Override
			public void handle (Object source, CarEvent.Type type, CarEvent.Order order) {
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
			// player.carState.event.addListener(this, CarStateEvent.Type.onTileChanged);
			GameEvents.driftState.addListener(driftStateListener, PlayerDriftStateEvent.Type.onBeginDrift);
			GameEvents.driftState.addListener(driftStateListener, PlayerDriftStateEvent.Type.onEndDrift);

			GameEvents.playerCar.addListener(playerCarListener, CarEvent.Type.onCollision);
			GameEvents.playerCar.addListener(playerCarListener, CarEvent.Type.onComputeForces);
			GameEvents.playerCar.addListener(playerCarListener, CarEvent.Type.onOutOfTrack);
			GameEvents.playerCar.addListener(playerCarListener, CarEvent.Type.onBackInTrack);
		}

		public void unregisterPlayerEvents () {
			// player.carState.event.removeListener(this, CarStateEvent.Type.onTileChanged);
			GameEvents.driftState.removeListener(driftStateListener, PlayerDriftStateEvent.Type.onBeginDrift);
			GameEvents.driftState.removeListener(driftStateListener, PlayerDriftStateEvent.Type.onEndDrift);

			GameEvents.playerCar.removeListener(playerCarListener, CarEvent.Type.onCollision);
			GameEvents.playerCar.removeListener(playerCarListener, CarEvent.Type.onComputeForces);
			GameEvents.playerCar.removeListener(playerCarListener, CarEvent.Type.onOutOfTrack);
			GameEvents.playerCar.removeListener(playerCarListener, CarEvent.Type.onBackInTrack);
		}

		public void registerGhostEvents () {
			GameEvents.ghostCars.addListener(ghostListener, CarEvent.Type.onGhostFadingOut);
		}

		public void unregisterGhostEvents () {
			GameEvents.ghostCars.removeListener(ghostListener, CarEvent.Type.onGhostFadingOut);
		}

	}
}
