package com.bitfire.uracer.audio;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.bitfire.uracer.carsimulation.CarForces;
import com.bitfire.uracer.carsimulation.CarInputMode;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.events.CarListener;
import com.bitfire.uracer.events.DriftStateListener;
import com.bitfire.uracer.game.GameData;

public class CarSoundManager implements DriftStateListener, CarListener {
	// sound effects
	private CarDriftSoundEffect carDrift;
	private CarEngineSoundEffect carEngine;
	private CarImpactSoundEffect carImpact;

	public CarSoundManager() {
		carEngine = new CarEngineSoundEffect();
		// carEngine.start();

		carDrift = new CarDriftSoundEffect();
		carDrift.start();	// wtf? why the manager should start it in load()?

		carImpact = new CarImpactSoundEffect();
	}

	public void dispose() {
		// carEngine.dispose();
		carDrift.dispose();
		carImpact.dispose();
	}

	public void tick() {
		if( GameData.playerState.car.getInputMode() == CarInputMode.InputFromPlayer ) {
			// TODO when update() will use GameData shared data internally, no params, thus a task-based component
			// system can be created
			// carEngine.update( player.currSpeedFactor );
			carDrift.update( GameData.playerState.currSpeedFactor );
		}
	}

	@Override
	public void onBeginDrift() {
		carDrift.driftBegin();
	}

	@Override
	public void onEndDrift() {
		carDrift.driftEnd();
	}

	@Override
	public void onComputeForces( CarForces forces ) {
	}

	@Override
	public void onCollision( Car car, Fixture other, Vector2 impulses ) {
		carImpact.impact( impulses.len(), GameData.playerState.currSpeedFactor );
	}

	public void reset() {
		// carEngine.reset();
		carDrift.reset();
	}
}
