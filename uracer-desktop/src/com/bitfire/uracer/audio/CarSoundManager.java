package com.bitfire.uracer.audio;

import com.bitfire.uracer.carsimulation.CarInputMode;
import com.bitfire.uracer.game.GameData;

public class CarSoundManager {
	// sound effects
	private CarDriftSoundEffect carDrift;
//	private CarEngineSoundEffect carEngine;
	public CarImpactSoundEffect carImpact;	// FIXME not public but proper GameData.playerState construction..

	public CarSoundManager() {
//		carEngine = new CarEngineSoundEffect();
		// carEngine.start();

		carDrift = new CarDriftSoundEffect();
		carDrift.start();

		carImpact = new CarImpactSoundEffect();
	}

	public void dispose() {
		// carEngine.dispose();
		carDrift.dispose();
		carImpact.dispose();
	}

	public void tick() {
		if( GameData.playerState.car.getInputMode() == CarInputMode.InputFromPlayer ) {
			// FIXME when update() will use GameData shared data internally, no params, thus a task-based component
			// system can be created
			// FIXME task-based system will cause the CarSoundManager to be only an instance store! Yay! Components for the better!
			// carEngine.update( player.currSpeedFactor );
			carDrift.update( GameData.playerState.currSpeedFactor );
		}
	}

	public void reset() {
		// carEngine.reset();
		carDrift.reset();
	}
}
