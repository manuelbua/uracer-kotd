
package com.bitfire.uracer.game.logic.replaying;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.CarForces;
import com.bitfire.uracer.game.logic.replaying.ReplayRecorder.RecorderError;

/** Manage player's performance recordings and keeps basic lap information */
public class LapManager implements Disposable {

	private final String levelId;
	private final ReplayRecorder recorder;
	private final ReplayBufferManager bufferManager;
	private Replay lastRecordedReplay;

	public LapManager (UserProfile userProfile, String levelId) {
		this.levelId = levelId;
		recorder = new ReplayRecorder(userProfile.userId);
		bufferManager = new ReplayBufferManager(userProfile.userId);
		lastRecordedReplay = null;
	}

	@Override
	public void dispose () {
		recorder.dispose();
	}

	/** Stops recording and invalidates last recorded replay */
	public void reset () {
		abortRecording();
		lastRecordedReplay = null;
	}

	/** Returns the Replay instance where the last recording took place */
	public Replay getLastRecordedReplay () {
		return lastRecordedReplay;
	}

	/** Starts recording the player lap performance. Returns the Replay instance where the recording is being performed. */
	public Replay startRecording (Car car) {
		if (recorder.isRecording()) {
			Gdx.app.log("TrackLapManager", "Couldn't start recording since it's already started.");
			return null;
		}

		Replay next = bufferManager.getNextBuffer();
		recorder.beginRecording(car, next, levelId);
		return next;
	}

	/** Add and record the specified CarForces */
	public RecorderError record (CarForces forces) {
		if (recorder.isRecording()) {
			return recorder.add(forces);
		}

		return RecorderError.RecordingNotEnabled;
	}

	/** Returns whether or not the lap manager is recording the player's performance */
	public boolean isRecording () {
		return recorder.isRecording();
	}

	/** Ends recording the previously started lap performance */
	public void stopRecording () {
		if (recorder.isRecording()) {

			// ends recording and keeps track of the last recorded replay
			lastRecordedReplay = recorder.endRecording();

			bufferManager.updateReplays();
		}
	}

	/** Discard the performance currently being recorded so far */
	public void abortRecording () {
		recorder.reset();
	}

	public float getCurrentReplaySeconds () {
		return recorder.getElapsedSeconds();
	}
}
