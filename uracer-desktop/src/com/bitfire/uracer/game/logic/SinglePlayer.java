
package com.bitfire.uracer.game.logic;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.postprocessing.PostProcessor;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.configuration.Storage;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.game.GameplaySettings;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.debug.DebugHelper;
import com.bitfire.uracer.game.debug.DebugHelper.RenderFlags;
import com.bitfire.uracer.game.debug.GameTrackDebugRenderer;
import com.bitfire.uracer.game.debug.player.DebugPlayer;
import com.bitfire.uracer.game.logic.gametasks.messager.Message;
import com.bitfire.uracer.game.logic.gametasks.messager.Message.Position;
import com.bitfire.uracer.game.logic.gametasks.messager.Message.Size;
import com.bitfire.uracer.game.logic.replaying.Replay;
import com.bitfire.uracer.game.logic.replaying.ReplayManager.ReplayInfo;
import com.bitfire.uracer.game.logic.types.helpers.CameraShaker;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.screens.GameScreensFactory.ScreenType;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.screen.TransitionFactory.TransitionType;
import com.bitfire.uracer.utils.CarUtils;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.OrdinalUtils;
import com.bitfire.uracer.utils.URacerRuntimeException;

public class SinglePlayer extends BaseLogic {
	protected DebugHelper debug = null;
	private boolean saving = false;
	private CameraShaker camShaker = new CameraShaker();
	private int selectedBestReplayIdx = 0;

	public SinglePlayer (UserProfile userProfile, GameWorld gameWorld, GameRenderer gameRenderer) {
		super(userProfile, gameWorld, gameRenderer);
		setupDebug(gameRenderer.getPostProcessing().getPostProcessor());
	}

	@Override
	public void dispose () {
		destroyDebug();
		super.dispose();
	}

	private void setupDebug (PostProcessor postProcessor) {
		if (Config.Debug.UseDebugHelper) {
			debug = new DebugHelper(gameWorld, postProcessor, lapManager, this, inputSystem);
			debug.add(new GameTrackDebugRenderer(RenderFlags.TrackSectors, gameWorld.getGameTrack()));
			debug.add(new DebugPlayer(RenderFlags.PlayerCarInfo, gameTasksManager));
			Gdx.app.debug("Game", "Debug helper initialized");
		}
	}

	private void destroyDebug () {
		if (Config.Debug.UseDebugHelper) {
			debug.dispose();
		}
	}

	private GhostCar findGhostFor (Replay replay) {
		for (int g = 0; g < ghostCars.length; g++) {
			GhostCar ghost = ghostCars[g];
			if (ghost != null && replay != null && ghost.getReplay().getReplayId().equals(replay.getReplayId())) {
				return ghost;
			}
		}

		return null;
	}

	private boolean hasGhostFor (Replay replay) {
		return (findGhostFor(replay) != null);
	}

	@Override
	public void handleExtraInput () {
		Input i = inputSystem;

		if (i.isPressed(Keys.O)) {
			removePlayer();
			restartGame();
			restartAllReplays();
		} else if (i.isPressed(Keys.P)) {
			addPlayer();
			restartGame();
		} else if (i.isPressed(Keys.D)) {
			boolean newstate = !gameRenderer.isDebugEnabled();
			gameRenderer.setDebug(newstate);
			debug.setEnabled(newstate);
		} else if (i.isPressed(Keys.TAB)) {
			// choose next/prev best target
			boolean backward = i.isOn(Keys.SHIFT_LEFT) || i.isOn(Keys.SHIFT_RIGHT);

			// retrieve the ghostcar index whose replay is the next/prev best as this one
			// if (!isWarmUp() && isCurrentLapValid)
			{

				GhostCar prevTarget = getNextTarget();
				int maxreplays = lapManager.getReplays().size;
				int maxtries = maxreplays;

				GhostCar next = null;
				boolean found = false;

				do {
					if (backward) {
						selectedBestReplayIdx--;
					} else {
						selectedBestReplayIdx++;
					}

					if (selectedBestReplayIdx < 0) selectedBestReplayIdx = maxreplays - 1;
					if (selectedBestReplayIdx == maxreplays) selectedBestReplayIdx = 0;

					next = getNextTarget();
					found = true;// next != null && next.hasReplay() && !next.getTrackState().ghostArrived;
				} while (maxtries-- >= 0 && !found);

				if (!isWarmUp() && found) {
					if (prevTarget != next && next.isPlaying()) {
						playerTasks.hudPlayer.highlightNextTarget(next);
						Gdx.app.log("SinglePlayer", "Next target index is #" + selectedBestReplayIdx);
					}
				}
			}
		}

	}

	@Override
	public GhostCar getNextTarget () {
		if (lapManager.getReplays().size > 0) {
			return findGhostFor(lapManager.getReplays().get(selectedBestReplayIdx));
		}

		return null;
	}

	private void saveReplay (final Replay replay) {
		if (saving) {
			Gdx.app.log("SinglePlayer", "(already saving, request ignored...");
			return;
		}

		if (replay != null) {
			saving = true;
			Thread savingThread = new Thread(new Runnable() {
				@Override
				public void run () {
					if (replay.save()) {
						Gdx.app.log("SinglePlayer", "Replay #" + replay.getShortReplayId() + " saved to \"" + replay.filename() + "\"");
					}
				}
			});

			savingThread.start();
			saving = false;
		}
	}

	@Override
	public void updateCameraPosition (Vector2 positionPx) {
		if (hasPlayer()) {
			// update player's headlights and move the world camera to follows it, if there is a player
			if (gameWorld.isNightMode()) {
				gameWorldRenderer.updatePlayerHeadlights(playerCar);
			}
			positionPx.set(playerCar.state().position);
			positionPx.add(camShaker.compute(getCollisionFactor()));
		} else if (isGhostActive(0)) {
			// FIXME use available/choosen replay
			positionPx.set(getGhost(0).state().position);
		} else {
			// no ghost, no player, WTF?
			positionPx.set(Convert.mt2px(gameWorld.playerStart.position));
		}
	}

	private boolean pruneReplay (Replay replay) {
		if (replay != null) {
			if (replay.delete()) {
				Gdx.app.log("SinglePlayer", "Pruned #" + replay.getShortReplayId());
				return true;
			}
		}

		return false;
	}

	/** Load from disk all the replays for the specified trackId, pruning while loading respecting the ReplayManager.MaxReplays
	 * constant. Any previous Replay will be cleared from the lapManager instance. */
	private int loadReplaysFromDiskFor (String trackId) {
		lapManager.removeAllReplays();

		int reloaded = 0;
		for (FileHandle userdir : Gdx.files.external(Storage.ReplaysRoot + gameWorld.getLevelId()).list()) {
			if (userdir.isDirectory()) {
				for (FileHandle userreplay : userdir.list()) {
					Replay replay = Replay.load(userreplay.path());
					if (replay != null) {
						ReplayInfo ri = lapManager.addReplay(replay);
						if (ri.accepted) {
							pruneReplay(ri.removed);
							reloaded++;
							Gdx.app.log("SinglePlayer", "Loaded replay #" + ri.replay.getShortReplayId());
						}
					}
				}
			}
		}

		Gdx.app.log("SinglePlayer", "Building opponents list:");

		for (int g = 0; g < ghostCars.length; g++) {
			ghostCars[g].removeReplay();
		}

		int pos = 1;
		int ghostIndex = 0;
		for (Replay r : lapManager.getReplays()) {
			ghostCars[ghostIndex].setReplay(r);
			ghostLapMonitor[ghostIndex].reset();

			Gdx.app.log("SinglePlayer",
				"#" + pos + ", #" + r.getShortReplayId() + ", secs=" + String.format("%02.03f", r.getTrackTimeInt() / 1000f)
					+ ", ct=" + r.getCreationTimestamp());
			pos++;
			ghostIndex++;
		}

		Gdx.app.log("SinglePlayer", "Reloaded " + reloaded + " opponents.");
		return reloaded;
	}

	@Override
	public void restartGame () {
		Gdx.app.log("SinglePlayer", "Starting/restarting game");
		super.restartGame();
		loadReplaysFromDiskFor(gameWorld.getLevelId());
	}

	@Override
	public void resetGame () {
		Gdx.app.log("SinglePlayer", "Resetting game");
		super.resetGame();
		messager.show("Game reset", 1.5f, Message.Type.Information, Position.Bottom, Size.Big);
		loadReplaysFromDiskFor(gameWorld.getLevelId());
	}

	@Override
	public void warmUpStarted () {
		messager.show("Warm up!", 1.5f, Message.Type.Information, Position.Top, Size.Big);
	}

	@Override
	public void warmUpCompleted () {
		messager.show("GOOOO!!", 1.5f, Message.Type.Information, Position.Top, Size.Big);
	}

	@Override
	public void playerLapStarted () {
		lapManager.stopRecording();
		playerCar.resetDistanceAndSpeed(true, false);
		lapManager.startRecording(playerCar, gameWorld.getLevelId(), userProfile.userId);
		restartAllReplays();
	}

	@Override
	public void playerLapCompleted () {
		if (lapManager.isRecording()) {
			ReplayInfo ri = lapManager.stopRecording();
			Replay r = ri.replay;

			if (ri.accepted) {

				CarUtils.dumpSpeedInfo("SinglePlayer", "Replay #" + r.getShortReplayId() + " accepted, player", playerCar,
					r.getTrackTime());

				saveReplay(r);
				pruneReplay(ri.removed);

				// show message
				int pos = ri.position;
				messager.show("You finished\n" + pos + OrdinalUtils.getOrdinalFor(pos) + "!", 1.5f, Message.Type.Information,
					Position.Middle, Size.Big);
			} else {
				String msg = "";
				String id = "(#" + r.getShortReplayId() + ")";
				float duration = 1.5f;

				switch (ri.reason) {
				case Null:
					msg = "Discarding null replay " + id;
					duration = 3;
					break;
				case InvalidMinDuration:
					msg = "Invalid lap (" + r.getTrackTime() + "s < " + GameplaySettings.ReplayMinDurationSecs + "s) " + id;
					duration = 10;
					break;
				case Invalid:
					msg = "The specified replay is not valid. (#" + r.getShortReplayId() + ") " + id;
					duration = 10;
					break;
				case WrongTrack:
					msg = "The specified replay belongs to another game track " + id;
					duration = 10;
					break;
				case Slower:
					msg = "Too slow!";
					duration = 1.5f;
					break;
				case NotDiscarded:
					break;
				}

				Gdx.app.log("SinglePlayer", msg);
				messager.show(msg, duration, Message.Type.Information, Position.Top, Size.Big);
			}
		}

		playerCar.resetDistanceAndSpeed(true, false);
	}

	@Override
	public void ghostLapCompleted (GhostCar ghost) {
		if (!hasPlayer()) {
			restartAllReplays();
		} else {
			// remove replay but do not reset its track state yet
			// ghost.removeReplay();
			ghost.stop();
		}
	}

	@Override
	public void ghostReplayStarted (GhostCar ghost) {
		if (ghost == findGhostFor(lapManager.getReplays().get(selectedBestReplayIdx))) {
			// ghost.setAlpha(Config.Graphics.DefaultTargetCarOpacity);
			playerTasks.hudPlayer.highlightNextTarget(ghost);
		}
	}

	@Override
	public void ghostReplayEnded (GhostCar ghost) {
		// can't stop the ghostcar here since it would stop the physics simulation for the GhostCar! Use the ghost lap completion
		// monitor instead!
		// ghost.stop();

		// CarUtils.dumpSpeedInfo("SinglePlayer", "GhostCar #" + ghost.getId(), ghost, ghost.getReplay().getTrackTime());

		// do the same as ghostfadingout
		if (ghost != null && ghost == getNextTarget()) {
			playerTasks.hudPlayer.unHighlightNextTarget();
		}
	}

	@Override
	public void doQuit () {
		lapManager.abortRecording(false);

		URacer.Screens.setScreen(ScreenType.MainScreen, TransitionType.Fader, 300);
		// URacer.Screens.setScreen(ScreenType.ExitScreen, TransitionType.Fader, 300);

		getTimeModulator().reset();
		URacer.Game.resetTimeModFactor();
	}

	//

	/** Restart all replays in the lap manager, if no next target set the best replay's car to it */
	private void restartAllReplays () {
		if (!(lapManager.getReplays().size <= ghostCars.length)) {
			throw new URacerRuntimeException("Replays count mismatch");
		}

		int g = 0;
		for (Replay r : lapManager.getReplays()) {
			GhostCar ghost = ghostCars[g];
			if (ghost == null) {
				throw new URacerRuntimeException("Ghost not ready (#" + g + ")");
			}

			ghost.setReplay(r);
			ghost.start();
			ghostLapMonitor[g].reset();

			// if no nextTarget then take the best (first)
			if (getNextTarget() == null && g == 0) {
				selectedBestReplayIdx = 0;
			}

			ghost.setAlpha(getNextTarget() == ghost ? Config.Graphics.DefaultTargetCarOpacity
				: Config.Graphics.DefaultGhostCarOpacity);

			g++;
		}
	}
}
