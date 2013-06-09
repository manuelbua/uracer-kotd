
package com.bitfire.uracer.game.logic.replaying;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.game.Time;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.CarForces;

public final class ReplayRecorder implements Disposable {
	// @off
	public enum RecorderError {
		NoError,
		RecordingNotEnabled,
		ReplayMemoryLimitReached
	}
	// @on

	// private final long userId;
	private boolean isRecording;
	private Time time;

	// replay data
	private Replay replay;

	public ReplayRecorder (long userId) {
		// this.userId = userId;
		isRecording = false;
		replay = null;
		time = new Time();
	}

	@Override
	public void dispose () {
		reset();
		time.dispose();
	}

	public void reset () {
		isRecording = false;
		time.reset();

		// ensure data is discarded
		if (replay != null) {
			replay.reset();
			replay = null;
		}
	}

	public void beginRecording (Car car, Replay replay, String levelId) {
		Gdx.app.log("Recorder", "Beginning recording #" + System.identityHashCode(replay));

		isRecording = true;
		this.replay = replay;
		replay.begin(levelId, car);
		time.start();
	}

	public RecorderError add (CarForces f) {
		if (!isRecording) {
			return RecorderError.RecordingNotEnabled;
		}

		if (!replay.add(f)) {
			return RecorderError.ReplayMemoryLimitReached;
		}

		return RecorderError.NoError;
	}

	public Replay endRecording () {
		if (!isRecording) {
			Gdx.app.log("Recorder", "Cannot end a recording that wasn't enabled!");
			return null;
		}

		time.stop();
		replay.end(time.elapsed(Time.Reference.TickSeconds));
		isRecording = false;

		Gdx.app.log("Recorder", "Finished recording #" + System.identityHashCode(replay));
		return replay;
	}

	public boolean isRecording () {
		return isRecording;
	}

	public float getElapsedSeconds () {
		if (isRecording) {
			return time.elapsed(Time.Reference.TickSeconds);
		}

		return 0;
	}
}
