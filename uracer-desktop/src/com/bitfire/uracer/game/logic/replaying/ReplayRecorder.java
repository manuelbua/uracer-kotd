
package com.bitfire.uracer.game.logic.replaying;

import com.badlogic.gdx.Gdx;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.CarForces;

public final class ReplayRecorder {
	// @off
	public enum RecorderError {
		NoError,
		RecordingNotEnabled,
		ReplayMemoryLimitReached
	}
	// @on

	private final long userId;
	private boolean isRecording;

	// replay data
	private Replay replay;

	public ReplayRecorder (long userId) {
		this.userId = userId;
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

	public void beginRecording (Car car, Replay replay, String trackId) {
		isRecording = true;
		this.replay = replay;
		Gdx.app.log("Recorder", "Beginning recording #" + System.identityHashCode(replay));
		replay.begin(trackId, car);
	}

	public RecorderError add (CarForces f) {
		if (!isRecording) {
			Gdx.app.log("Recorder", "Cannot add event, recording not enabled!");
			return RecorderError.RecordingNotEnabled;
		}

		if (!replay.add(f)) {
			// Gdx.app.log("Recorder", "Replay memory limit reached (" + Replay.MaxEvents + " events), restarting.");
			return RecorderError.ReplayMemoryLimitReached;
		}

		return RecorderError.NoError;
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
