package com.bitfire.uracer.game.logic.post;

import com.bitfire.uracer.game.player.PlayerCar;

public interface Animator {

	public void update( PlayerCar player );
	public void reset();
}
