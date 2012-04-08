package com.bitfire.uracer.audio;

import com.bitfire.uracer.events.GameLogicEvent;
import com.bitfire.uracer.events.GameLogicEvent.Type;
import com.bitfire.uracer.game.GameLogic;
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
		GameLogic.event.addListener( gameLogicEvent );

		// carEngine = new CarEngineSoundEffect();
		// carEngine.start();

		carDrift = new CarDriftSoundEffect();
		carDrift.onStart();

		carImpact = new CarImpactSoundEffect();
	}

	public static final float timeDilationToAudioPitch( float pitchIn, float timeMultiplier ) {
		return pitchIn - (1 - timeMultiplier) * 0.3f;
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
