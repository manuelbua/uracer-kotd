
package com.bitfire.uracer.game.logic.types;

import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.game.logic.replaying.Replay;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.rendering.GameWorldRenderer;
import com.bitfire.uracer.game.world.GameWorld;

public class SinglePlayer extends CommonLogic {

	public SinglePlayer (GameWorld gameWorld, GameRenderer gameRenderer, ScalingStrategy scalingStrategy) {
		super(gameWorld, gameRenderer, scalingStrategy);
	}

	public void setBestLocalReplay (Replay replay) {
		restartGame();
		lapManager.setBestReplay(replay);
		// if( !hasPlayer() )
		{
			ghostCar.setReplay(replay);
		}
	}

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

}
