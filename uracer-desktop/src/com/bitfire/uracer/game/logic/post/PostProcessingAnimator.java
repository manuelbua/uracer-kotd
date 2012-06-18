package com.bitfire.uracer.game.logic.post;

import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.player.PlayerCar;

public interface PostProcessingAnimator {

	public void update( PlayerCar player, GhostCar ghost );
	public void reset();
}
