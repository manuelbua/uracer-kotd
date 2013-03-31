
package com.bitfire.uracer.game.logic.types;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.MathUtils;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.configuration.Gameplay;
import com.bitfire.uracer.configuration.Gameplay.TimeDilateInputMode;
import com.bitfire.uracer.configuration.UserPreferences;
import com.bitfire.uracer.configuration.UserPreferences.Preference;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.game.DebugHelper;
import com.bitfire.uracer.game.GameLogic;
import com.bitfire.uracer.game.GameplaySettings;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.CarEvent;
import com.bitfire.uracer.game.actors.CarPreset;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.logic.gametasks.GameTasksManager;
import com.bitfire.uracer.game.logic.gametasks.hud.elements.HudPlayer.EndDriftType;
import com.bitfire.uracer.game.logic.helpers.CarFactory;
import com.bitfire.uracer.game.logic.helpers.GameTrack;
import com.bitfire.uracer.game.logic.helpers.PlayerGameTasks;
import com.bitfire.uracer.game.logic.post.PostProcessing;
import com.bitfire.uracer.game.logic.post.PostProcessing.Effects;
import com.bitfire.uracer.game.logic.post.animators.AggressiveCold;
import com.bitfire.uracer.game.logic.post.ssao.Ssao;
import com.bitfire.uracer.game.logic.replaying.LapManager;
import com.bitfire.uracer.game.logic.replaying.Replay;
import com.bitfire.uracer.game.logic.replaying.ReplayManager;
import com.bitfire.uracer.game.logic.replaying.ReplayRecorder.RecorderError;
import com.bitfire.uracer.game.logic.types.common.LapCompletionMonitor;
import com.bitfire.uracer.game.logic.types.common.LapCompletionMonitor.LapCompletionMonitorListener;
import com.bitfire.uracer.game.logic.types.common.TimeModulator;
import com.bitfire.uracer.game.logic.types.common.WrongWayMonitor;
import com.bitfire.uracer.game.logic.types.common.WrongWayMonitor.WrongWayMonitorListener;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.player.PlayerDriftStateEvent;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.rendering.GameWorldRenderer;
import com.bitfire.uracer.game.screens.GameScreensFactory.ScreenType;
import com.bitfire.uracer.game.tween.GameTweener;
import com.bitfire.uracer.game.tween.SysTweener;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.screen.TransitionFactory.TransitionType;
import com.bitfire.uracer.utils.CarUtils;
import com.bitfire.uracer.utils.NumberString;

public abstract class CommonLogic implements GameLogic, CarEvent.Listener, PlayerDriftStateEvent.Listener,
	WrongWayMonitorListener, LapCompletionMonitorListener {
	// input
	protected Input input = null;

	// world
	protected GameWorld gameWorld = null;
	protected GameTrack gameTrack = null;

	// rendering
	private GameRenderer gameRenderer = null;
	protected GameWorldRenderer gameWorldRenderer = null;
	protected PostProcessing postProcessing = null;

	// player
	protected final UserProfile userProfile;
	protected PlayerCar playerCar = null;
	protected GhostCar[] ghostCars = new GhostCar[ReplayManager.MaxReplays];
	private WrongWayMonitor wrongWayMonitor;
	protected boolean isCurrentLapValid = true;
	protected boolean isWarmUpLap = true;

	// lap
	protected LapManager lapManager = null;
	private LapCompletionMonitor lapMonitor = null;

	// tasks
	protected GameTasksManager gameTasksManager = null;
	protected PlayerGameTasks playerTasks = null;

	// time modulation logic
	protected boolean timeDilation;
	private TimeModulator timeMod = null;
	private TimeDilateInputMode timeDilateMode;

	protected ReplayManager replayManager;

	public CommonLogic (UserProfile userProfile, GameWorld gameWorld, GameRenderer gameRenderer, ScalingStrategy scalingStrategy) {
		this.userProfile = userProfile;
		this.gameWorld = gameWorld;
		this.gameRenderer = gameRenderer;
		this.gameWorldRenderer = gameRenderer.getWorldRenderer();
		this.input = URacer.Game.getInputSystem();
		timeDilateMode = Gameplay.TimeDilateInputMode.valueOf(UserPreferences.string(Preference.TimeDilateInputMode));
		timeMod = new TimeModulator();

		Gdx.app.log("GameLogic", "Tweening helpers created");

		// post-processing
		postProcessing = new PostProcessing(gameWorld, gameRenderer);

		if (gameRenderer.hasPostProcessor()) {
			postProcessing.addAnimator(AggressiveCold.Name, new AggressiveCold(this, postProcessing, gameWorld.isNightMode()));
			postProcessing.enableAnimator(AggressiveCold.Name);
		}

		Gdx.app.log("GameLogic", "Post-processing animator created");

		// main game tasks
		gameTasksManager = new GameTasksManager(gameWorld, scalingStrategy);
		gameTasksManager.createTasks();

		// player tasks
		playerTasks = new PlayerGameTasks(userProfile, gameTasksManager, scalingStrategy);

		lapManager = new LapManager(userProfile, gameWorld.getTrackId());
		for (int i = 0; i < ReplayManager.MaxReplays; i++) {
			ghostCars[i] = CarFactory.createGhost(i, gameWorld, CarPreset.Type.L1_GoblinOrange);
			ghostCars[i].event.addListener(this, CarEvent.Type.onGhostFadingOut);
			// ghostCars[i].carState.event.addListener(this, CarStateEvent.Type.onTileChanged);
		}

		replayManager = new ReplayManager(userProfile, gameWorld.getTrackId());
		gameTrack = gameWorld.getGameTrack();

		wrongWayMonitor = new WrongWayMonitor(this);
		lapMonitor = new LapCompletionMonitor(this, gameTrack);

		// messager.show( "THIS IS SOME FINE SHIT", 60, Message.Type.Information,
		// MessagePosition.Bottom, MessageSize.Big );
	}

	@Override
	public void dispose () {
		removePlayer();
		gameTrack.dispose();
		gameTasksManager.dispose();
		playerTasks.dispose();

		if (playerCar != null) {
			playerCar.dispose();
		}

		for (int i = 0; i < ReplayManager.MaxReplays; i++) {
			if (ghostCars[i] != null) {
				ghostCars[i].event.removeListener(this, CarEvent.Type.onGhostFadingOut);
				// ghostCars[i].carState.event.removeListener(this, CarStateEvent.Type.onTileChanged);
				ghostCars[i].dispose();
			}
		}

		GameTweener.dispose();
		replayManager.dispose();
	}

	//
	// specific game logic shall also implement these
	//

	// implementers should returns the camera zoom amount
	protected abstract float updateCamera (float timeModFactor);

	protected abstract void gameRestart ();

	protected abstract void gameReset ();

	protected abstract void newReplay (Replay replay);

	protected abstract void discardedReplay (Replay replay);

	protected abstract void lapStarted ();

	protected abstract void lapCompleted ();

	protected abstract void driftBegins ();

	protected abstract void driftEnds ();

	protected abstract boolean timeDilationAvailable ();

	protected abstract void timeDilationBegins ();

	protected abstract void timeDilationEnds ();

	protected abstract void collision ();

	protected abstract void outOfTrack ();

	protected abstract void backInTrack ();

	protected abstract void ghostFadingOut (Car ghost);

	protected void wrongWayBegins () {
		postProcessing.alertWrongWayBegins(500);
		Gdx.app.log("CommonLogic", "wrong way begin");
	}

	protected void wrongWayEnds () {
		postProcessing.alertWrongWayEnds(500);
	}

	//
	// SHARED OPERATIONS (Subclass Sandbox pattern)
	//

	/** Restarts the current game */
	protected void restartGame () {
		restartLogic();
		gameWorldRenderer.setInitialCameraPositionOrient(playerCar);
		// updateCamera(0);
		// gameWorldRenderer.updateCamera();

		// 3..2..1.. playerLapComplete()!
		// onLapComplete();

		// raise event
		gameRestart();
	}

	/** Restart and completely resets the game, removing any playing replay so far */
	protected void resetGame () {
		resetLogic();
		gameWorldRenderer.setInitialCameraPositionOrient(playerCar);
		// updateCamera(0);
		// gameWorldRenderer.updateCamera();

		// 3..2..1.. playerLapComplete()!
		// onLapComplete();

		// raise event
		gameReset();
	}

	/** Request time dilation to begin */
	protected void requestTimeDilationStart () {
		timeDilation = true;
		timeMod.toDilatedTime();
		timeDilationBegins();
	}

	/** Request time dilation to end */
	protected void requestTimeDilationFinish () {
		timeDilation = false;
		timeMod.toNormalTime();
		timeDilationEnds();
	}

	/** Sets the player from the specified preset */
	@Override
	public void setPlayer (CarPreset.Type presetType) {
		if (hasPlayer()) {
			Gdx.app.log("GameLogic", "A player already exists.");
			return;
		}

		playerCar = CarFactory.createPlayer(gameWorld, presetType);

		configurePlayer(gameWorld, input /* gameTasksManager.input */, playerCar);
		Gdx.app.log("GameLogic", "Player configured");

		playerTasks.createTasks(playerCar, lapManager.getLapInfo(), gameRenderer);
		Gdx.app.log("GameLogic", "Game tasks created and configured");

		registerPlayerEvents(playerCar);
		Gdx.app.log("GameLogic", "Registered player-related events");

		updateCamera(0);
		gameWorldRenderer.updateCamera();

		postProcessing.setPlayer(playerCar);
		gameWorldRenderer.setRenderPlayerHeadlights(gameWorld.isNightMode());

		gameWorldRenderer.showDebugGameTrack(Config.Debug.RenderTrackSectors);
		gameWorldRenderer.setGameTrackDebugCar(playerCar);

		restartGame();

		if (Config.Debug.UseDebugHelper) {
			DebugHelper.setPlayer(playerCar);
		}
	}

	protected void removePlayer () {
		if (!hasPlayer()) {
			Gdx.app.log("GameLogic", "There is no player to remove.");
			return;
		}

		// setting a null player (disabling player), unregister
		// previously registered events, if there was a player
		if (playerCar != null) {
			unregisterPlayerEvents(playerCar);
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

	private void registerPlayerEvents (PlayerCar player) {
		// player.carState.event.addListener(this, CarStateEvent.Type.onTileChanged);
		player.driftState.event.addListener(this, PlayerDriftStateEvent.Type.onBeginDrift);
		player.driftState.event.addListener(this, PlayerDriftStateEvent.Type.onEndDrift);
		player.event.addListener(this, CarEvent.Type.onCollision);
		player.event.addListener(this, CarEvent.Type.onComputeForces);
		player.event.addListener(this, CarEvent.Type.onOutOfTrack);
		player.event.addListener(this, CarEvent.Type.onBackInTrack);
	}

	private void unregisterPlayerEvents (PlayerCar player) {
		// player.carState.event.removeListener(this, CarStateEvent.Type.onTileChanged);
		player.driftState.event.removeListener(this, PlayerDriftStateEvent.Type.onBeginDrift);
		player.driftState.event.removeListener(this, PlayerDriftStateEvent.Type.onEndDrift);
		player.event.removeListener(this, CarEvent.Type.onCollision);
		player.event.removeListener(this, CarEvent.Type.onComputeForces);
		player.event.removeListener(this, CarEvent.Type.onOutOfTrack);
		player.event.removeListener(this, CarEvent.Type.onBackInTrack);
	}

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

		timeDilation = false;
		timeMod.reset();
		SysTweener.clear();
		GameTweener.clear();
		lapManager.abortRecording();
		gameTasksManager.restart();

		wrongWayMonitor.reset();
		isCurrentLapValid = true;
		isWarmUpLap = true;

		postProcessing.resetAnimator();

		// playerTasks.playerEngineSoundFx.start();
		playerTasks.playerDriftSoundFx.start();
		playerTasks.hudLapInfo.toDefaultColor();
		playerTasks.hudLapInfo.setValid(true);
		playerTasks.hudPlayer.trackProgress.resetCounters(false);

		lapMonitor.reset();
		gameTrack.setInitialCarSector(playerCar);
		lapMonitor.setCar(playerCar);
	}

	private void resetLogic () {
		// clean everything
		replayManager.reset();
		lapManager.abortRecording();
		lapManager.reset();
		gameTasksManager.reset();

		restartLogic();
	}

	//
	// implement interfaces and listeners callbacks
	//

	@Override
	public void tick () {
		processInput();
	}

	@Override
	public void tickCompleted () {
		gameTasksManager.physicsStep.onSubstepCompleted();

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
			boolean wrongWay = wrongWayMonitor.isWrongWay();
			isCurrentLapValid = !wrongWay;

			// blink on wrong way (keeps calling, returns earlier if busy)
			if (wrongWay) {
				playerTasks.hudPlayer.highlightWrongWay();
			}

			// blink on out of track (keeps calling, returns earlier if busy)
			if (playerCar.isOutOfTrack()) {
				playerTasks.hudPlayer.highlightOutOfTrack();
			}
		}
	}

	@Override
	public void beforeRender () {
		URacer.timeMultiplier = timeMod.getTime();
		float zoom = updateCamera(URacer.Game.getTimeModFactor());

		Ssao ssao = (Ssao)postProcessing.getEffect(Effects.Ssao.name);
		// CameraMotion mot = (CameraMotion)postProcessing.getEffect(Effects.MotionBlur.name);

		if (gameWorld.isNightMode() && postProcessing.hasEffect(Effects.Crt.name)) {
			gameWorldRenderer.setAmbientColor(0.1f, 0.05f, 0.1f, 0.6f + 0.2f * URacer.Game.getTimeModFactor());
			gameWorldRenderer.setTreesAmbientColor(0.1f, 0.05f, 0.1f, 0.5f + 0.5f * URacer.Game.getTimeModFactor());
		} else {
			gameWorldRenderer.setAmbientColor(0.1f, 0.05f, 0.15f, 0.4f + 0.2f * URacer.Game.getTimeModFactor());
			gameWorldRenderer.setTreesAmbientColor(0.1f, 0.1f, 0.15f, 0.4f + 0.5f * URacer.Game.getTimeModFactor());
		}

		// camera/ray handler update
		gameWorldRenderer.updateCamera();

		// post-processing step / SSAO
		if (ssao != null) {
			ssao.setEnabled(true);
			ssao.setNormalDepthMap(gameWorldRenderer.getNormalDepthMap().getColorBufferTexture());
		}

		// post-processing step / MOTION BLUR
		// if (mot != null) {
		// Camera cam = GameEvents.gameRenderer.camPersp;
		// float blur_scale = MathUtils.clamp(((float)Gdx.graphics.getFramesPerSecond() / 60.0f), 0, 1);
		// mot.setEnabled(true);
		// mot.setNearFar(cam.near, cam.far);
		// mot.setMatrices(gameWorldRenderer.getInvView(), gameWorldRenderer.getPrevViewProj(), gameWorldRenderer.getInvProj());
		// mot.setNormalDepthMap(gameWorldRenderer.getNormalDepthMap().getColorBufferTexture());
		// mot.setBlurScale(blur_scale);
		// mot.setBlurPasses(4);
		// mot.setDepthScale(40);
		// }

		// Gdx.app.log("CommonLogic", "wuc=" + lapMonitor.getWarmUpCompletion());
		postProcessing.onBeforeRender(zoom, MathUtils.clamp(lapMonitor.getWarmUpCompletion(), 0, 1));

		// game tweener step
		GameTweener.update();
	}

	private Replay userRec = null; // dbg on-demand rec/play via Z/X

	private void processInput () {
		// fast car switch (debug!)
		for (int i = Keys.NUM_1; i <= Keys.NUM_9; i++) {
			if (input.isPressed(i)) {
				CarPreset.Type type = CarPreset.Type.values()[i - Keys.NUM_1];
				removePlayer();
				setPlayer(type);
			}
		}

		if (input.isPressed(Keys.C)) {

			if (lapManager.getBestReplay() != null) {
				resetAllGhosts();
				getGhost(0).setReplay(lapManager.getBestReplay());
			}

		} else if (input.isPressed(Keys.R)) {

			// restart
			restartGame();

		} else if (input.isPressed(Keys.T)) {

			// reset
			resetGame();

		} else if (input.isPressed(Keys.Z)) {

			// FIXME this should go in some sort of DebugLogic thing..

			// start recording
			playerCar.resetDistanceAndSpeed(true, true);
			resetAllGhosts();
			lapManager.abortRecording();
			userRec = lapManager.startRecording(playerCar);
			Gdx.app.log("GameLogic", "Recording...");

		} else if (input.isPressed(Keys.X)) {

			// FIXME this should go in some sort of DebugLogic thing..

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

		} else if (input.isPressed(Keys.Q) || input.isPressed(Keys.ESCAPE) || input.isPressed(Keys.BACK)) {

			lapManager.stopRecording();
			gameTasksManager.sound.stop();

			// quit

			URacer.Screens.setScreen(ScreenType.MainScreen, TransitionType.Fader, 500);
			// URacer.Screens.setScreen( ScreenType.ExitScreen, TransitionType.Fader, 500 );

			timeMod.reset();
			URacer.Game.resetTimeModFactor();

		} else if (input.isPressed(Keys.O)) {
			// FIXME this should go in some sort of DebugLogic thing..
			removePlayer();
		} else if (input.isPressed(Keys.P)) {
			// FIXME this should go in some sort of DebugLogic thing..
			setPlayer(CarPreset.Type.L1_GoblinOrange);
		} else if (input.isPressed(Keys.W)) {
			// FIXME this should go in some sort of DebugLogic thing..
			Config.Debug.RenderBox2DWorldWireframe = !Config.Debug.RenderBox2DWorldWireframe;
		} else if (input.isPressed(Keys.B)) {
			// FIXME this should go in some sort of DebugLogic thing..
			Config.Debug.Render3DBoundingBoxes = !Config.Debug.Render3DBoundingBoxes;
		} else if (input.isPressed(Keys.TAB)) {
			// FIXME this should go in some sort of DebugLogic thing..
			Config.Debug.RenderTrackSectors = !Config.Debug.RenderTrackSectors;
			gameWorldRenderer.showDebugGameTrack(Config.Debug.RenderTrackSectors);
			gameWorldRenderer.setGameTrackDebugCar(playerCar);
		}

		switch (timeDilateMode) {
		case Toggle:
			if (input.isPressed(Keys.SPACE) || input.isTouched(1)) {
				timeDilation = !timeDilation;

				if (timeDilation) {
					if (timeDilationAvailable()) {
						requestTimeDilationStart();
					} else {
						timeDilation = false;
					}
				} else {
					requestTimeDilationFinish();
				}
			}
			break;

		case TouchAndRelease:

			if (input.isPressed(Keys.SPACE) || input.isTouched(1)) {
				if (!timeDilation && timeDilationAvailable()) {
					timeDilation = true;
					requestTimeDilationStart();
				}
			} else if (input.isReleased(Keys.SPACE) || input.isUntouched(1)) {
				if (timeDilation) {
					timeDilation = false;
					requestTimeDilationFinish();
				}
			}
			break;
		}
	}

	//
	// EVENT HANDLERS
	//

	@Override
	public void carEvent (CarEvent.Type type, CarEvent.Data data) {
		switch (type) {
		case onCollision:

			// invalidate drifting
			if (playerCar.driftState.isDrifting) {
				playerCar.driftState.invalidateByCollision();
			}

			// invalidate time modulation
			if (timeDilation) {
				requestTimeDilationFinish();
			}

			postProcessing.alertCollision(0.75f, 4000);

			collision();
			break;
		case onOutOfTrack:
			outOfTrack();
			break;
		case onBackInTrack:
			backInTrack();
			break;
		case onComputeForces:
			if (lapManager.record(data.forces) == RecorderError.ReplayMemoryLimitReached) {

			}
			break;
		case onGhostFadingOut:
			ghostFadingOut(data.car);
			break;
		}
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

	// NOTE: no events for the GhostCar since we record the forces generated by the CarSimulator
	@Override
	public void playerDriftStateEvent (PlayerCar player, PlayerDriftStateEvent.Type type) {
		switch (type) {
		case onBeginDrift:
			playerTasks.hudPlayer.beginDrift();
			driftBegins();
			break;
		case onEndDrift:
			driftEnds();

			float driftSeconds = player.driftState.driftSeconds();
			String msgSeconds = NumberString.format(playerCar.driftState.driftSeconds()) + "  seconds!";

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

		if (!lapManager.isRecording()) {
			// new race, only begin recording
			lapManager.startRecording(playerCar);
		}

		// lap is started, warmup has ended
		isWarmUpLap = false;

		playerCar.resetDistanceAndSpeed(true, false);
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
			Gdx.app.log("CommonLogic", "Invalid lap detected, too short (" + lapManager.getLapInfo().getElapsedSeconds() + "sec < "
				+ GameplaySettings.ReplayMinDurationSecs + ")");
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
}
