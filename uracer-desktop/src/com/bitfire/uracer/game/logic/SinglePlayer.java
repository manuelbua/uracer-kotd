
package com.bitfire.uracer.game.logic;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.logic.gametasks.messager.Message;
import com.bitfire.uracer.game.logic.gametasks.messager.Message.Position;
import com.bitfire.uracer.game.logic.gametasks.messager.Message.Size;
import com.bitfire.uracer.game.logic.replaying.Replay;
import com.bitfire.uracer.game.logic.replaying.ReplayManager;
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

		if (replay != null && replay.isValid()) {
			saving = true;
			Thread savingThread = new Thread(new Runnable() {
				@Override
				public void run () {
					replay.save(replay.getReplayId());
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

	@Override
	public void restartGame () {
		super.restartGame();
		Gdx.app.log("SinglePlayer", "Starting/restarting game");
	}

	@Override
	public void resetGame () {
		super.resetGame();
		messager.show("Game reset", 1.5f, Message.Type.Information, Position.Bottom, Size.Big);
		Gdx.app.log("SinglePlayer", "Resetting game");
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

			int pos = ReplayManager.MaxReplays + 1;
			if (ri.accepted) {
				Replay r = ri.replay;
				CarUtils.dumpSpeedInfo("Player", playerCar, r.getTrackTime());

				saveReplay(r);

				// prune old, if any
				if (ri.removed != null) {
					ri.removed.delete();
					Gdx.app.log("SinglePlayer", "Pruned " + ri.removed.getReplayId());
				}

				// show message
				pos = ri.position;
				float v = gameTrack.getTrackCompletion(playerCar);
				Gdx.app.log("SinglePlayer", "Stopped player at " + v);
			}

			messager.show("You finished\n" + pos + OrdinalUtils.getOrdinalFor(pos) + "!", 1.5f, Message.Type.Information,
				Position.Middle, Size.Big);
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

		float v = gameTrack.getTrackCompletion(ghost);
		Gdx.app.log("SinglePlayer", "Stopped ghost #" + ghost.getId() + " at " + v);
	}

	@Override
	public void ghostReplayEnded (GhostCar ghost) {
		Gdx.app.log("SinglePlayer", "Replay finished for ghost #" + ghost.getId() + ", waiting for lap monitor to act...");
		CarUtils.dumpSpeedInfo("GhostCar #" + ghost.getId(), ghost, ghost.getReplay().getTrackTime());
	}
}
