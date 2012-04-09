package com.bitfire.uracer.carsimulation;

import com.bitfire.uracer.events.GameLogicEvent;
import com.bitfire.uracer.game.GameData;
import com.bitfire.uracer.game.GameData.Events;
import com.bitfire.uracer.game.entities.Car;

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
		Events.gameLogic.addListener( gameLogicEvent );
		isRecording = false;
		replay = null;
	}

	public void reset() {
		isRecording = false;
		replay = null;
	}

	public void beginRecording( Car car, Replay replay, /* long startTimeNs, */String trackName ) {
		isRecording = true;
		this.replay = replay;
		replay.begin( trackName, GameData.gameSettings.difficulty, car );
	}

	public void add( CarForces f ) {
		if( !isRecording ) {
			// System.out.println("Cannot add event, recording not enabled!");
			return;
		}

		if( !replay.add( f ) ) {
			System.out.println( "Replay memory limit reached (" + replay.MaxEvents + " events), restarting." );
		}
	}

	public void endRecording() {
		if( !isRecording ) {
			// System.out.println("Cannot end a recording that wasn't enabled!");
			return;
		}

		replay.end();
		// float secs = (float)(System.nanoTime() - replay.trackStartTimeNs) / 1000000000f;
		// secs *= URacer.timeMultiplier;
		// replay.setReplayData( name, GameData.gameSettings.difficulty, replay.time.elapsed( Time.Reference.Ticks ));

		// System.out.println( "Recorded " + replay.getEventsCount() + " events" );
		// System.out.println("Recorded " + secs + " seconds" );
		// System.out.println("Time: " +
		// replay.time.elapsed( Time.Reference.Absolute ) + " abs, " +
		// replay.time.elapsed( Time.Reference.NumberOfTicks ) + " ticks, " +
		// replay.time.elapsed( Time.Reference.Ticks ) + " secs" );

		isRecording = false;
		replay = null;
	}

	public boolean isRecording() {
		return isRecording;
	}
}