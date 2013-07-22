
package com.bitfire.uracer.game.logic.replaying;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.CarForces;
import com.bitfire.uracer.game.logic.replaying.ReplayManager.ReplayResult;
import com.bitfire.uracer.game.logic.replaying.ReplayRecorder.RecorderError;

/** Manage player's performance recordings and keeps basic lap information */
public class LapManager implements Disposable {

	private final ReplayRecorder recorder;
	private final ReplayManager manager;

	public LapManager (String currentTrackId) {
		recorder = new ReplayRecorder();
		manager = new ReplayManager(currentTrackId);
	}

	@Override
	public void dispose () {
		manager.dispose();
		recorder.dispose();
	}

	/** Stops recording and invalidates last recorded replay, optionally resetting the record timer */
	public void reset (boolean resetTimer) {
		abortRecording(resetTimer);
	}

	public void resetTimer () {
		recorder.resetTimer();
	}

	public ReplayResult addReplay (Replay replay) {
		return manager.addReplay(replay);
	}

	/** Starts recording the player lap performance. Returns the Replay instance where the recording is being performed. */
	public void startRecording (Car car, String trackId, String userId) {
		if (recorder.isRecording()) {
			Gdx.app.log("TrackLapManager", "Couldn't start recording since it's already started.");
			return;
		}

		recorder.beginRecording(car, trackId, userId);
	}

	/** Add and record the specified CarForces */
	public RecorderError record (CarForces forces) {
		if (recorder.isRecording()) {
			return recorder.add(forces);
		}

		return RecorderError.RecordingNotEnabled;
	}

	/** Ends the previously started recording and returns its Replay, if any */
	public Replay stopRecording () {
		if (recorder.isRecording()) {
			return recorder.endRecording();
		}

		return null;
	}

	/** Discard the performance currently being recorded */
	public void abortRecording (boolean resetTimer) {
		recorder.reset();
		if (resetTimer) {
			resetTimer();
		}
	}

	/** Returns whether or not the lap manager is recording the player's performance */
	public boolean isRecording () {
		return recorder.isRecording();
	}

	public int getCurrentReplayTicks () {
		return recorder.getElapsedTicks();
	}

	public Array<Replay> getReplays () {
		return manager.getReplays();
	}

	public Replay getReplay (String replayId) {
		return manager.getById(replayId);
	}

	public int getReplaysCount () {
		return manager.getReplaysCount();
	}

	public Replay getBestReplay () {
		return manager.getBestReplay();
	}

	public void removeAllReplays () {
		manager.removeAll();
	}
}
