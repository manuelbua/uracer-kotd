package com.bitfire.uracer.game.logic;

import com.bitfire.uracer.game.Time;

public final class TrackLapInfo {
	// replays
	private Time time;
	private long startTimeNs;
	private float lastTrackTimeSecs;
	private float bestTrackTimeSecs;
	private boolean hasLastTrackTimeSecs;
	private boolean hasBestTrackTimeSecs;

	public TrackLapInfo() {

		startTimeNs = 0;
		lastTrackTimeSecs = 0;
		hasLastTrackTimeSecs = false;
		hasBestTrackTimeSecs = false;
		time = new Time();

		reset();
	}

	public void reset() {
		hasLastTrackTimeSecs = false;
		hasBestTrackTimeSecs = false;
		time.start();
		startTimeNs = System.nanoTime();
	}

	public long restart() {
		startTimeNs = System.nanoTime();
		time.start();
		return startTimeNs;
	}

	public void setLastTrackTimeSeconds( float value ) {
		lastTrackTimeSecs = value;
		hasLastTrackTimeSecs = true;
	}

	public void setBestTrackTimeSeconds( float value ) {
		bestTrackTimeSecs = value;
		hasBestTrackTimeSecs = true;
	}

	public float getElapsedSeconds() {
		return time.elapsed( Time.Reference.TickSeconds );
	}

	public boolean hasLastTrackTimeSeconds() {
		return hasLastTrackTimeSecs;
	}

	public float getLastTrackTimeSeconds() {
		return lastTrackTimeSecs;
	}

	public boolean hasBestTrackTimeSeconds() {
		return hasBestTrackTimeSecs;
	}

	public float getBestTrackTimeSeconds() {
		return bestTrackTimeSecs;
	}
}
