package com.bitfire.uracer.game.logic;

import com.bitfire.uracer.game.Time;

/** Encapsulates the track's lap information, such as elapsed time so far, best
 * lap times and last lap's time.
 *
 * @author bmanuel */
public final class TrackLapInfo {
	private Time time;
	private float lastTrackTimeSecs;
	private float bestTrackTimeSecs;
	private boolean hasLastTrackTimeSecs;
	private boolean hasBestTrackTimeSecs;

	public TrackLapInfo() {
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
	}

	public void restart() {
		time.start();
	}

	public float getElapsedSeconds() {
		return time.elapsed( Time.Reference.TickSeconds );
	}

	public void setLastTrackTimeSeconds( float value ) {
		lastTrackTimeSecs = value;
		hasLastTrackTimeSecs = true;
	}

	public void setBestTrackTimeSeconds( float value ) {
		bestTrackTimeSecs = value;
		hasBestTrackTimeSecs = true;
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
