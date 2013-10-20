
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

	private boolean isRecording;
	private Time time;

	// freshly recorded data
	private Replay recording;

	public ReplayRecorder () {
		isRecording = false;
		recording = new Replay();
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

	public void resetTimer () {
		time.reset();
	}

	public void beginRecording (Car car, String trackId, String userId) {
		isRecording = true;
		recording.begin(trackId, userId, car);
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
			Gdx.app.log("Recorder", "Cannot end a recording that never began.");
			return null;
		}
		time.stop();
		recording.end((int)(time.elapsed().ticks));
		isRecording = false;
		return recording;
	}

	public boolean isRecording () {
		return isRecording;
	}

	public int getElapsedTicks () {
		return (int)(time.elapsed().ticks);
	}
}
