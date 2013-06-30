
package com.bitfire.uracer.game.logic;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.configuration.Storage;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.game.GameplaySettings;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.logic.gametasks.messager.Message;
import com.bitfire.uracer.game.logic.gametasks.messager.Message.Position;
import com.bitfire.uracer.game.logic.gametasks.messager.Message.Size;
import com.bitfire.uracer.game.logic.replaying.Replay;
import com.bitfire.uracer.game.logic.replaying.ReplayManager.ReplayInfo;
import com.bitfire.uracer.game.logic.types.helpers.CameraShaker;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.utils.CarUtils;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.OrdinalUtils;

public class SinglePlayer extends BaseLogic {
	private boolean saving = false;
	private CameraShaker camShaker = new CameraShaker();

	public SinglePlayer (UserProfile userProfile, GameWorld gameWorld, GameRenderer gameRenderer) {
		super(userProfile, gameWorld, gameRenderer);
	}

	@Override
	public void dispose () {
		super.dispose();
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

	/** Load and add to the LapManager all the replies for the specified trackId */
	private int refreshAllReplaysFor (String trackId) {
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

		Gdx.app.log("SinglePlayer", "Opponents list:");
		int pos = 1;
		for (Replay r : lapManager.getReplays()) {
			Gdx.app.log("SinglePlayer",
				"#" + pos + ", #" + r.getShortReplayId() + ", tt=" + r.getTrackTimeInt() + ", ct=" + r.getCreationTimestamp());
			pos++;

		}
		return reloaded;
	}

	@Override
	public void restartGame () {
		Gdx.app.log("SinglePlayer", "Starting/restarting game");
		super.restartGame();

		int reloaded = refreshAllReplaysFor(gameWorld.getLevelId());
		Gdx.app.log("SinglePlayer", "Reloaded " + reloaded + " opponents.");
	}

	@Override
	public void resetGame () {
		Gdx.app.log("SinglePlayer", "Resetting game");
		super.resetGame();
		messager.show("Game reset", 1.5f, Message.Type.Information, Position.Bottom, Size.Big);

		int reloaded = refreshAllReplaysFor(gameWorld.getLevelId());
		Gdx.app.log("SinglePlayer", "Reloaded " + reloaded + " opponents.");
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
				messager.show(msg, duration, Message.Type.Information, Position.Middle, Size.Big);
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
			ghost.removeReplay();
		}
	}

	@Override
	public void ghostReplayEnded (GhostCar ghost) {
		CarUtils.dumpSpeedInfo("SinglePlayer", "GhostCar #" + ghost.getId(), ghost, ghost.getReplay().getTrackTime());
	}
}
