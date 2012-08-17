
package com.bitfire.uracer.game.logic.types;

import com.badlogic.gdx.Gdx;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.game.logic.gametasks.messager.Message.Position;
import com.bitfire.uracer.game.logic.gametasks.messager.Message.Size;
import com.bitfire.uracer.game.logic.gametasks.messager.Message.Type;
import com.bitfire.uracer.game.logic.gametasks.messager.Messager;
import com.bitfire.uracer.game.logic.replaying.Replay;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.rendering.GameWorldRenderer;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.CarUtils;
import com.bitfire.uracer.utils.NumberString;

public class SinglePlayerLogic extends CommonLogic {

	public SinglePlayerLogic (GameWorld gameWorld, GameRenderer gameRenderer, ScalingStrategy scalingStrategy) {
		super(gameWorld, gameRenderer, scalingStrategy);
	}

	//
	// utilities
	//
	private void setBestLocalReplay () {
		Replay replay = Replay.loadLocal(gameWorld.levelName);
		if (replay == null) {
			return;
		}

		lapManager.setAsBestReplay(replay);
		ghostCar.setReplay(replay);
	}

	//
	// event listeners / callbacks
	//

	@Override
	protected void updateCamera (float timeModFactor) {
		gameWorldRenderer.setCameraZoom(1.0f + (GameWorldRenderer.MaxCameraZoom - 1) * timeModFactor);

		// update player's headlights and move the world camera to follows it, if there is a player
		if (hasPlayer()) {

			if (gameWorld.isNightMode()) {
				gameWorldRenderer.updatePlayerHeadlights(playerCar);
			}

			gameWorldRenderer.setCameraPosition(playerCar.state().position, playerCar.state().orientation,
				playerCar.carState.currSpeedFactor);

		} else if (ghostCar.hasReplay()) {

			gameWorldRenderer.setCameraPosition(ghostCar.state().position, ghostCar.state().orientation, 0);

		} else {

			// no ghost, no player, WTF?
			gameWorldRenderer.setCameraPosition(gameWorld.playerStartPos, gameWorld.playerStartOrient, 0);
		}
	}

	@Override
	protected void restart () {
		Gdx.app.log("SinglePlayerLogic", "Starting/restarting game");
		setBestLocalReplay();
	}

	@Override
	protected void reset () {
		Gdx.app.log("SinglePlayerLogic", "Resetting game");
	}

	@Override
	public void newReplay () {
		Messager messager = gameTasksManager.messager;
		Replay replay = lapManager.getLastRecordedReplay();

		if (!lapManager.hasAllReplays()) {
			// only one single valid replay

			ghostCar.setReplay(replay);
			replay.saveLocal(messager);
			messager.show("GO!  GO!  GO!", 3f, Type.Information, Position.Middle, Size.Big);

		} else {

			// both valid, replay best, overwrite worst

			Replay best = lapManager.getBestReplay();
			Replay worst = lapManager.getWorstReplay();

			float bestTime = AMath.round(best.trackTimeSeconds, 2);
			float worstTime = AMath.round(worst.trackTimeSeconds, 2);
			float diffTime = AMath.round(worstTime - bestTime, 2);

			if (AMath.equals(worstTime, bestTime)) {
				// draw!
				messager.show("DRAW!", 3f, Type.Information, Position.Top, Size.Big);
			} else {
				// has the player managed to beat the best lap?
				if (lapManager.isLastBestLap()) {
					messager.show("-" + NumberString.format(diffTime) + " seconds!", 3f, Type.Good, Position.Top, Size.Big);
				} else {
					messager.show("+" + NumberString.format(diffTime) + " seconds", 3f, Type.Bad, Position.Top, Size.Big);
				}
			}

			ghostCar.setReplay(best);
			best.saveLocal(messager);
		}

		CarUtils.dumpSpeedInfo("Player", playerCar, replay.trackTimeSeconds);
	}
}
