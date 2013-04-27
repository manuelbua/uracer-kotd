
package com.bitfire.uracer.game.logic.replaying;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.CarForces;
import com.bitfire.uracer.game.logic.LapInfo;
import com.bitfire.uracer.game.logic.replaying.ReplayRecorder.RecorderError;

/** Records player laps and keeps updated information for the current lap. */
public class LapManager implements Disposable {

	private final String levelId;
	private final ReplayRecorder recorder;
	private final ReplayBufferManager bufferManager;
	private final LapInfo lapInfo;
	private Replay lastRecordedReplay;

	public LapManager (UserProfile userProfile, String levelId) {
		this.levelId = levelId;
		recorder = new ReplayRecorder(userProfile.userId);
		lapInfo = new LapInfo();
		bufferManager = new ReplayBufferManager(userProfile.userId);
		lastRecordedReplay = null;
	}

	@Override
	public void dispose () {
		recorder.reset();
	}

	// operations

	/** Discard the performance currently being recorded so far */
	public void abortRecording () {
		recorder.reset();
		lapInfo.reset();
	}

	/** Reset any recorded replay so far */
	public void reset () {
		lastRecordedReplay = null;
		recorder.reset();
		lapInfo.resetTime();
		bufferManager.reset();
	}

	public void setAsBestReplay (Replay replay) {
		bufferManager.setAsBestReplay(replay);
		lapInfo.setBestTrackTimeSeconds(replay.trackTimeSeconds);
	}

	// getters

	/** Returns the LapInfo information regarding the currently active lap */
	public LapInfo getLapInfo () {
		return lapInfo;
	}

	/** Returns whether or not the Best or Worst replay is available */
	public boolean hasAnyReplay () {
		return bufferManager.hasAnyReplayData();
	}

	/** Returns the first available, and valid, replay */
	public Replay getAnyReplay () {
		return bufferManager.getAnyReplay();
	}

	/** Returns whether or not the Best and Worst replays are available */
	public boolean hasAllReplays () {
		return bufferManager.hasAllReplayData();
	}

	/** Returns the best replay available, so far */
	public Replay getBestReplay () {
		return bufferManager.getBestReplay();
	}

	/** Returns the worst replay available, so far */
	public Replay getWorstReplay () {
		return bufferManager.getWorstReplay();
	}

	/** Returns the Replay instance where the last recording took place */
	public Replay getLastRecordedReplay () {
		return lastRecordedReplay;
	}

	/** Returns whether or not the last recorded lap was the best one */
	public boolean isLastBestLap () {
		// return lastRecordedReplay.id == bufferManager.getBestReplay().id;
		if (bufferManager.getBestReplay() != null) {
			return (System.identityHashCode(lastRecordedReplay) == System.identityHashCode(bufferManager.getBestReplay()));
		}

		return false;
	}

	/** Returns whether or not a best lap is present */
	public boolean hasBestLapReplay () {
		return bufferManager.hasBestReplay();
	}

	/** Returns whether or not a worst lap is present */
	public boolean hasWorstLapReplay () {
		return bufferManager.hasWorstReplay();
	}

	/** Starts recording the player lap performance. Returns the Replay instance where the recording is being performed. */
	public Replay startRecording (Car car) {
		if (recorder.isRecording()) {
			Gdx.app.log("TrackLapManager", "Couldn't start recording since it's already started.");
			return null;
		}

		Replay next = bufferManager.getNextBuffer();
		lapInfo.start();
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
			lapInfo.stop();

			// ends recording and keeps track of the last recorded replay
			lastRecordedReplay = recorder.endRecording();

			bufferManager.updateReplays();

			// update lap info with last lap times
			if (bufferManager.hasAllReplayData()) {
				// lap finished, update lapinfo with the last recorded replay
				lapInfo.setLastTrackTimeSeconds(lastRecordedReplay.trackTimeSeconds);

			} else {
				// lap finished, update lapinfo with whatever replay data is available
				lapInfo.setLastTrackTimeSeconds(bufferManager.getAnyReplay().trackTimeSeconds);
			}

			// update lap info with best lap time
			lapInfo.setBestTrackTimeSeconds(bufferManager.getBestReplay().trackTimeSeconds);
		}
	}
}
