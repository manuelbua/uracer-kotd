package com.bitfire.uracer.game.audio;

import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.events.GameLogicEvent;
import com.bitfire.uracer.game.events.GameLogicEvent.Type;
import com.bitfire.uracer.task.Task;

public class CarSoundManager extends Task {
	private CarDriftSoundEffect carDrift;
	// private CarEngineSoundEffect carEngine;
	private CarImpactSoundEffect carImpact;

	private final GameLogicEvent.Listener gameLogicEvent = new GameLogicEvent.Listener() {
		@Override
		public void gameLogicEvent( Type type ) {
			switch( type ) {
			case onRestart:
			case onReset:
				reset();
				break;
			}
		}
	};

	public CarSoundManager() {
		GameEvents.gameLogic.addListener( gameLogicEvent );

		// carEngine = new CarEngineSoundEffect();
		// carEngine.start();

		carDrift = new CarDriftSoundEffect();
		carDrift.onStart();

		carImpact = new CarImpactSoundEffect();
	}

	@Override
	public void dispose() {
		// carEngine.dispose();
		carDrift.onDispose();
		carImpact.onDispose();
	}

	public void reset() {
		// carEngine.reset();
		carDrift.onReset();
	}

	@Override
	protected void onTick() {
		carDrift.onTick();
	}
}
