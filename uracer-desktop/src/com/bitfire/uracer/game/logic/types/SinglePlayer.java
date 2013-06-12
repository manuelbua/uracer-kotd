
package com.bitfire.uracer.game.logic.types;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.game.logic.gametasks.messager.Message;
import com.bitfire.uracer.game.logic.gametasks.messager.Message.Position;
import com.bitfire.uracer.game.logic.gametasks.messager.Message.Size;
import com.bitfire.uracer.game.logic.replaying.Replay;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.utils.CarUtils;
import com.bitfire.uracer.utils.Convert;

public class SinglePlayer extends CommonLogic {
	public SinglePlayer (UserProfile userProfile, GameWorld gameWorld, GameRenderer gameRenderer) {
		super(userProfile, gameWorld, gameRenderer);
	}

	@Override
	public void dispose () {
		super.dispose();
	}

	@Override
	protected void updateCameraPosition (Vector2 positionPx) {
		if (hasPlayer()) {
			// update player's headlights and move the world camera to follows it, if there is a player
			if (gameWorld.isNightMode()) {
				gameWorldRenderer.updatePlayerHeadlights(playerCar);
			}

			positionPx.set(playerCar.state().position);

		} else if (isGhostActive(0)) {
			// FIXME use best replay
			positionPx.set(getGhost(0).state().position);
		} else {
			// no ghost, no player, WTF?
			positionPx.set(Convert.mt2px(gameWorld.playerStart.position));
		}
	}

	// the game has been restarted
	@Override
	public void restartGame () {
		Gdx.app.log("SinglePlayer", "Starting/restarting game");
		super.restartGame();
	}

	// the game has been reset
	@Override
	public void resetGame () {
		Gdx.app.log("SinglePlayer", "Resetting game");
		super.resetGame();
		messager.show("Game reset", 1.5f, Message.Type.Information, Position.Bottom, Size.Big);
	}

	@Override
	public void newReplay (Replay replay) {
		messager.show("New record!", 1.5f, Message.Type.Information, Position.Bottom, Size.Big);
		CarUtils.dumpSpeedInfo("Player", playerCar, replay.getTrackTime());
	}

	@Override
	protected void discardedReplay () {
		messager.show("Try again...", 1.5f, Message.Type.Information, Position.Bottom, Size.Normal);
	}

	@Override
	protected void warmUpStarted () {
		messager.show("Warm up!", 1.5f, Message.Type.Information, Position.Top, Size.Big);
	}

	@Override
	protected void warmUpCompleted () {
		messager.show("GOOOO!!", 1.5f, Message.Type.Information, Position.Top, Size.Big);
	}

}
