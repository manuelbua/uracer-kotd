package com.bitfire.uracer.game.logic.replaying;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.game.GameplaySettings;
import com.bitfire.uracer.game.actors.CarForces;
import com.bitfire.uracer.game.logic.LapInfo;
import com.bitfire.uracer.game.logic.helpers.ReplayRecorder;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.world.GameWorld;

/** Manages to record player lap to Replay objects and keep tracks of lap information. */
public class LapManager implements Disposable {

	private GameWorld gameWorld;
	private GameplaySettings settings;
	private ReplayRecorder recorder;
	private ReplayBufferManager bufferManager;
	private LapInfo lapInfo;
	private Replay lastRecordedReplay;

	public LapManager( GameWorld gameWorld, GameplaySettings settings ) {
		this.gameWorld = gameWorld;
		this.settings = settings;

		recorder = new ReplayRecorder();
		lapInfo = new LapInfo();
		bufferManager = new ReplayBufferManager();
		lastRecordedReplay = null;
	}

	@Override
	public void dispose() {
		recorder.reset();
		recorder = null;
		lapInfo = null;
		bufferManager = null;
	}

	// operations

	/** Discard the performance currently being recorded so far */
	public void abortRecording() {
		recorder.reset();
	}

	/** Reset any recorded replay so far */
	public void reset() {
		lastRecordedReplay = null;
		lapInfo.resetTime();
		bufferManager.reset();
	}

	public void setBestReplay( Replay replay ) {
		bufferManager.setBestReplay( replay );
		lapInfo.setBestTrackTimeSeconds( replay.trackTimeSeconds );
	}

	// getters

	public LapInfo getLapInfo() {
		return lapInfo;
	}

	/** Returns whether or not the Best or Worst replay is available */
	public boolean hasAnyReplay() {
		return bufferManager.hasAnyReplayData();
	}

	/** Returns the first available, and valid, replay */
	public Replay getAnyReplay() {
		return bufferManager.getAnyReplay();
	}

	/** Returns whether or not the Best and Worst replays are available */
	public boolean hasAllReplays() {
		return bufferManager.hasAllReplayData();
	}

	/** Returns the best replay available, so far */
	public Replay getBestReplay() {
		return bufferManager.getBestReplay();
	}

	/** Returns the worst replay available, so far */
	public Replay getWorstReplay() {
		return bufferManager.getWorstReplay();
	}

	public Replay getLastRecordedReplay() {
		return lastRecordedReplay;
	}

	public boolean isLastBestLap() {
		return (lastRecordedReplay.id == bufferManager.getBestReplay().id);
	}

	public boolean hasBestLapReplay() {
		return bufferManager.hasBestReplay();
	}

	public boolean hasWorstLapReplay() {
		return bufferManager.hasWorstReplay();
	}

	// triggered from game logic

	public void onPlayerComputeForces( CarForces forces ) {
		if( recorder.isRecording() ) {
			recorder.add( forces );
		}
	}

	/** Starts recording the player lap performance.
	 * Returns the Replay instance where the recording is being performed. */
	public Replay startRecording( PlayerCar player ) {
		if( recorder.isRecording() ) {
			Gdx.app.log( "TrackLapManager", "Couldn't start recording since it's already started." );
			return null;
		}

		Replay next = bufferManager.getNextBuffer();
		lapInfo.restartTime();
		recorder.beginRecording( player, next, gameWorld.levelName, settings.difficulty );
		return next;
	}

	/** Ends recording the previously started lap performance */
	public void stopRecording() {
		if( recorder.isRecording() ) {
			// ends recording and keeps track of the last recorded replay
			lastRecordedReplay = recorder.endRecording();

			bufferManager.updateReplays();

			// update lap info with last lap times
			if( bufferManager.hasAllReplayData() ) {
				// lap finished, update lapinfo with the last recorded replay
				lapInfo.setLastTrackTimeSeconds( lastRecordedReplay.trackTimeSeconds );

			} else {
				// lap finished, update lapinfo with whatever replay data is available
				lapInfo.setLastTrackTimeSeconds( bufferManager.getAnyReplay().trackTimeSeconds );
			}

			// update lap info with best lap time
			lapInfo.setBestTrackTimeSeconds( bufferManager.getBestReplay().trackTimeSeconds );
		}
	}
}
