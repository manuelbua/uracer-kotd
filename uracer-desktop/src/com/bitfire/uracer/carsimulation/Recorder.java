package com.bitfire.uracer.carsimulation;

import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.game.GameData;

public class Recorder {
	private boolean isRecording;

	// replay data
	private Replay replay;

	public Recorder() {
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