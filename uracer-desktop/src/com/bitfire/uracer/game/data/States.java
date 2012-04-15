package com.bitfire.uracer.game.data;

import com.bitfire.uracer.game.states.DriftState;
import com.bitfire.uracer.game.states.LapState;

/** States */
public final class States {
//	public PlayerState player;
	public DriftState playerDrift;
	public LapState lap;

	public States(/*Car car*/) {
//		player = new PlayerState( car, CarFactory.createGhost( car ) );
		playerDrift = new DriftState();
		lap = new LapState();
	}

	public void dispose() {
	}
}