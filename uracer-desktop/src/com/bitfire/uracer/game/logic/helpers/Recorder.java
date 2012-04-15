package com.bitfire.uracer.game.logic.helpers;

import com.badlogic.gdx.Gdx;
import com.bitfire.uracer.game.GameDifficulty;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.Replay;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.CarForces;
import com.bitfire.uracer.game.events.GameLogicEvent;

public class Recorder {
	private boolean isRecording;

	// replay data
	private Replay replay;

	private final GameLogicEvent.Listener gameLogicEvent = new GameLogicEvent.Listener() {
		@Override
		public void gameLogicEvent( GameLogicEvent.Type type ) {
			switch( type ) {
			case onReset:
			case onRestart:
				reset();
				break;
			}
		}
	};

	public Recorder() {
		GameEvents.gameLogic.addListener( gameLogicEvent, GameLogicEvent.Type.onReset );
		GameEvents.gameLogic.addListener( gameLogicEvent, GameLogicEvent.Type.onRestart );
		isRecording = false;
		replay = null;
	}

	public void reset() {
		isRecording = false;
		replay = null;
	}

	public void beginRecording( Car car, Replay replay, String trackName, GameDifficulty gameDifficulty ) {
		isRecording = true;
		this.replay = replay;
		replay.begin( trackName, gameDifficulty, car );
	}

	public void add( CarForces f ) {
		if( !isRecording ) {
			Gdx.app.log( "Recorder", "Cannot add event, recording not enabled!" );
			return;
		}

		if( !replay.add( f ) ) {
			Gdx.app.log( "Recorder", "Replay memory limit reached (" + Replay.MaxEvents + " events), restarting." );
		}
	}

	public void endRecording() {
		if( !isRecording ) {
			Gdx.app.log( "Recorder", "Cannot end a recording that wasn't enabled!" );
			return;
		}

		replay.end();
		isRecording = false;
		replay = null;
	}

	public boolean isRecording() {
		return isRecording;
	}
}