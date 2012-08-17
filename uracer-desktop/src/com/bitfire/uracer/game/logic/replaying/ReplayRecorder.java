
package com.bitfire.uracer.game.logic.replaying;

import com.badlogic.gdx.Gdx;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.CarForces;

public final class ReplayRecorder {
	private boolean isRecording;

	// replay data
	private Replay replay;

	public ReplayRecorder () {
		isRecording = false;
		replay = null;
	}

	public void reset () {
		isRecording = false;

		// ensure data is discarded
		if (replay != null) {
			replay.reset();
			replay = null;
		}
	}

	public void beginRecording (Car car, Replay replay, String trackName) {
		isRecording = true;
		this.replay = replay;
		Gdx.app.log("Recorder", "Beginning recording #" + System.identityHashCode(replay));
		replay.begin(trackName, car);
	}

	public void add (CarForces f) {
		if (!isRecording) {
			Gdx.app.log("Recorder", "Cannot add event, recording not enabled!");
			return;
		}

		if (!replay.add(f)) {
			Gdx.app.log("Recorder", "Replay memory limit reached (" + Replay.MaxEvents + " events), restarting.");
		}
	}

	public Replay endRecording () {
		if (!isRecording) {
			Gdx.app.log("Recorder", "Cannot end a recording that wasn't enabled!");
			return null;
		}

		Gdx.app.log("Recorder", "Finished recording #" + System.identityHashCode(replay));
		Replay r = replay;
		replay.end();
		isRecording = false;
		replay = null;
		return r;
	}

	public boolean isRecording () {
		return isRecording;
	}
}
