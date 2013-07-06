
package com.bitfire.uracer.game.logic.gametasks;

import com.bitfire.uracer.game.player.PlayerCar;

public class PlayerClient {
	protected PlayerCar player;
	protected boolean hasPlayer;

	public void player (PlayerCar player) {
		this.player = player;
		this.hasPlayer = (player != null);
	}
}
