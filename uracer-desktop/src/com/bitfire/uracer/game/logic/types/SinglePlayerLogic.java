
package com.bitfire.uracer.game.logic.types;

import com.badlogic.gdx.Gdx;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.game.actors.CarPreset;
import com.bitfire.uracer.game.logic.gametasks.Messager;
import com.bitfire.uracer.game.logic.gametasks.messager.Message;
import com.bitfire.uracer.game.logic.gametasks.messager.Message.Position;
import com.bitfire.uracer.game.logic.gametasks.messager.Message.Size;
import com.bitfire.uracer.game.logic.replaying.Replay;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.rendering.GameWorldRenderer;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.CarUtils;
import com.bitfire.uracer.utils.Convert;

public class SinglePlayerLogic extends CommonLogic {

	private Messager messager;

	public SinglePlayerLogic (UserProfile userProfile, GameWorld gameWorld, GameRenderer gameRenderer) {
		super(userProfile, gameWorld, gameRenderer);
		messager = gameTasksManager.messager;
	}

	@Override
	public void setPlayer (CarPreset.Type presetType) {
		super.setPlayer(presetType);
	}

	@Override
	public void dispose () {
		super.dispose();
	}

	private float prevZoom = GameWorldRenderer.MinCameraZoom + GameWorldRenderer.ZoomWindow;
	private float previousSpeed = 0, previousDs = 0;

	// the camera needs to be positioned
	@Override
	protected float updateCamera (float timeModFactor) {
		float speedFactor = 0, driftStrength = 0;

		if (hasPlayer()) {
			speedFactor = AMath.fixup(AMath.lerp(previousSpeed, playerCar.carState.currSpeedFactor, 0.02f));
			previousSpeed = speedFactor;

			driftStrength = AMath.fixup(AMath.lerp(previousDs, playerCar.driftState.driftStrength, 0.02f));
			previousDs = driftStrength;
		}

		float minZoom = GameWorldRenderer.MinCameraZoom;
		float maxZoom = GameWorldRenderer.MaxCameraZoom;

		float cameraZoom = (minZoom + GameWorldRenderer.ZoomWindow);
		cameraZoom += (maxZoom - cameraZoom) * timeModFactor;
		cameraZoom += 0.25f * GameWorldRenderer.ZoomWindow * driftStrength;

		// cameraZoom = minZoom;

		cameraZoom = AMath.lerp(prevZoom, cameraZoom, 0.1f);
		cameraZoom = AMath.clampf(cameraZoom, minZoom, maxZoom);

		cameraZoom = AMath.fixupTo(cameraZoom, minZoom + GameWorldRenderer.ZoomWindow);
		// Gdx.app.log("", "zoom=" + cameraZoom);

		gameWorldRenderer.setCameraZoom(cameraZoom);
		prevZoom = cameraZoom;

		// update player's headlights and move the world camera to follows it, if there is a player
		if (hasPlayer()) {

			if (gameWorld.isNightMode()) {
				gameWorldRenderer.updatePlayerHeadlights(playerCar);
			}

			gameWorldRenderer.setCameraPosition(playerCar.state().position, playerCar.state().orientation,
				playerCar.carState.currSpeedFactor);

		} else if (getGhost(0) != null && getGhost(0).hasReplay()) {
			gameWorldRenderer.setCameraPosition(getGhost(0).state().position, getGhost(0).state().orientation, 0);
		} else {
			// no ghost, no player, WTF?
			gameWorldRenderer.setCameraPosition(Convert.mt2px(gameWorld.playerStart.position), gameWorld.playerStart.orientation, 0);
		}

		return cameraZoom;
	}

	// the game has been restarted
	@Override
	public void restartGame () {
		super.restartGame();

		Gdx.app.log("SinglePlayerLogic", "Starting/restarting game");
		gameTasksManager.messager.show("Game restarted", 3, Message.Type.Information, Position.Bottom, Size.Big);

		isPenalty = false;

	}

	// the game has been reset
	@Override
	public void resetGame () {
		super.resetGame();

		Gdx.app.log("SinglePlayerLogic", "Resetting game");
		gameTasksManager.messager.show("Game reset", 3, Message.Type.Information, Position.Bottom, Size.Big);

		replayManager.reset();
		isPenalty = false;
	}

	// a new Replay from the player is available: note that CommonLogic already perform
	// some basic filtering such as null checking, length validity, better-than-worst...
	@Override
	public void newReplay (Replay replay) {

		CarUtils.dumpSpeedInfo("Player", playerCar, replay.trackTimeSeconds);

		if (!replayManager.canClassify()) {
			getGhost(0).setReplay(replay);
			replay.saveLocal(messager);
			// messager.show("GO!  GO!  GO!", 3f, Type.Information, Position.Bottom, Size.Big);
		} else {
			Replay best = replayManager.getBestReplay();
			Replay worst = replayManager.getWorstReplay();

			float bestTime = AMath.round(best.trackTimeSeconds, 2);
			float worstTime = AMath.round(worst.trackTimeSeconds, 2);
			// float diffTime = AMath.round(worstTime - bestTime, 2);

			if (AMath.equals(worstTime, bestTime)) {
				// draw!
				// messager.show("DRAW!", 3f, Type.Information, Position.Bottom, Size.Big);
			} else {
				// has the player managed to beat the best lap?
				// if (lapManager.isLastBestLap()) {
				// messager.show("-" + NumberString.format(diffTime) + " seconds!", 3f, Type.Good, Position.Bottom, Size.Big);
				// } else {
				// messager.show("+" + NumberString.format(diffTime) + " seconds", 3f, Type.Bad, Position.Bottom, Size.Big);
				// }
			}
		}
	}
}
