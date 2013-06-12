
package com.bitfire.uracer.game.logic.gametasks;

import com.bitfire.uracer.game.player.PlayerCar;

public class PlayerClient {
	protected PlayerCar player;

	public void player (PlayerCar player) {
		this.player = player;
	}

	protected boolean hasPlayer () {
		return player != null;
	}
}
