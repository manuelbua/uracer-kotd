package com.bitfire.uracer.carsimulation;

import com.bitfire.uracer.URacer;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.game.GameData;

public class Recorder {
	private boolean isRecording;

	// replay data
	private Replay replay;
	private String name;

	public Recorder() {
		isRecording = false;
		replay = null;
		name = "";
	}

	public void reset() {
		isRecording = false;
		replay = null;
		name = "";
	}

	public void beginRecording( Car car, Replay replay, long startTimeNs, String name ) {
		isRecording = true;
		this.replay = replay;
		this.name = name;
		replay.clearForces();
		replay.setCarData( car );
		replay.trackStartTimeNs = startTimeNs;
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

		float secs = (float)(System.nanoTime() - replay.trackStartTimeNs) / 1000000000f;
		secs *= URacer.timeMultiplier;
		replay.setReplayData( name, GameData.gameSettings.difficulty, secs );

		// System.out.println( "Recorded " + replay.getEventsCount() + " events" );

		isRecording = false;
		replay = null;
	}

	public boolean isRecording() {
		return isRecording;
	}
}