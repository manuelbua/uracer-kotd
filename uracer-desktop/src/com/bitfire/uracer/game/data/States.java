package com.bitfire.uracer.game.data;

import com.bitfire.uracer.game.player.Car;
import com.bitfire.uracer.game.player.CarFactory;
import com.bitfire.uracer.game.states.DriftState;
import com.bitfire.uracer.game.states.LapState;
import com.bitfire.uracer.game.states.PlayerState;

/** States */
public final class States {
	public PlayerState player;
	public DriftState playerDrift;
	public LapState lap;

	public States(Car car) {
		player = new PlayerState( car, CarFactory.createGhost( car ) );
		playerDrift = new DriftState();
		lap = new LapState();
	}
	
	public void dispose() {
	}
}