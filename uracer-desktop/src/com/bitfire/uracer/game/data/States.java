package com.bitfire.uracer.game.data;

import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.CarFactory;
import com.bitfire.uracer.game.states.DriftState;
import com.bitfire.uracer.game.states.LapState;
import com.bitfire.uracer.game.states.PlayerState;

/** States */
public final class States {
	public PlayerState playerState;
	public DriftState driftState;
	public LapState lapState;

	public States(Car car) {
		playerState = new PlayerState( car, CarFactory.createGhost( car ) );
		driftState = new DriftState();
		lapState = new LapState();
	}
	
	public void dispose() {
	}
}