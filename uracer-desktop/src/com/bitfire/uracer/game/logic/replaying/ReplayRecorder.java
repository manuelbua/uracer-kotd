
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

	// freshly recorded data
	private Replay recording;

	public ReplayRecorder (long userId) {
		// this.userId = userId;
		isRecording = false;
		recording = new Replay(userId);
		time = new Time();
	}

	@Override
	public void dispose () {
		reset();
		time.dispose();
	}

	public void reset () {
		isRecording = false;
		time.stop();
		recording.reset();
	}

	public void beginRecording (Car car, String levelId) {
		Gdx.app.log("Recorder", "Beginning recording");

		isRecording = true;
		recording.begin(levelId, car);
		time.start();
	}

	public RecorderError add (CarForces f) {
		if (!isRecording) {
			return RecorderError.RecordingNotEnabled;
		}

		if (!recording.add(f)) {
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
		recording.end(time.elapsed(Time.Reference.TickSeconds));
		isRecording = false;

		Gdx.app.log("Recorder", "Finished recording");
		return recording;
	}

	public boolean isRecording () {
		return isRecording;
	}

	public float getElapsedSeconds () {
		return time.elapsed(Time.Reference.TickSeconds);
	}
}
