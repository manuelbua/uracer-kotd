
package com.bitfire.uracer.game.logic.replaying;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.game.Time;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.CarForces;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.URacerRuntimeException;

public final class ReplayRecorder implements Disposable {
	// @off
	public enum RecorderError {
		NoError,
		RecordingNotEnabled,
		ReplayMemoryLimitReached
	}
	// @on

	private MessageDigest replayDigest;
	private boolean isRecording;
	private Time time;

	// freshly recorded data
	private Replay recording;

	public ReplayRecorder () {
		isRecording = false;
		recording = new Replay();
		time = new Time();

		try {
			replayDigest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new URacerRuntimeException("No support for SHA-256 crypto has been found.");
		}
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
		Gdx.app.log("Recorder", "Beginning recording");

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

	private String computeId (Replay replay, float elapsed) {
		String trackTime = "" + ((int)(elapsed * AMath.ONE_ON_CMP_EPSILON));
		String utcTime = "" + (new Date()).getTime();

		replayDigest.reset();
		replayDigest.update(utcTime.getBytes());
		replayDigest.update(recording.getUserId().getBytes());
		replayDigest.update(recording.getTrackId().getBytes());
		replayDigest.update(trackTime.getBytes());

		String replayId = new BigInteger(1, replayDigest.digest()).toString(16);
		return replayId;
	}

	public Replay endRecording () {
		if (!isRecording) {
			Gdx.app.log("Recorder", "Cannot end a recording that never began.");
			return null;
		}

		time.stop();
		float elapsed = time.elapsed(Time.Reference.TickSeconds);
		recording.end(computeId(recording, elapsed), elapsed);
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
