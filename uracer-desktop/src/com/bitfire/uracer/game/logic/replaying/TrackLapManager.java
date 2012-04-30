package com.bitfire.uracer.game.logic.replaying;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.game.GameplaySettings;
import com.bitfire.uracer.game.actors.CarForces;
import com.bitfire.uracer.game.logic.TrackLapInfo;
import com.bitfire.uracer.game.logic.helpers.ReplayRecorder;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.world.GameWorld;

/** Manages to record player lap to Replay objects and keep tracks of lap information. */
public class TrackLapManager implements Disposable {

	private GameWorld gameWorld;
	private GameplaySettings settings;
	private ReplayRecorder recorder;
	private ReplayBuffer replayBuffer;
	private TrackLapInfo lapInfo;
	private Replay lastRecordedReplay;

	public TrackLapManager( GameWorld gameWorld, GameplaySettings settings ) {
		this.gameWorld = gameWorld;
		this.settings = settings;

		recorder = new ReplayRecorder();
		lapInfo = new TrackLapInfo();
		replayBuffer = new ReplayBuffer();
		lastRecordedReplay = null;
	}

	@Override
	public void dispose() {
		recorder.reset();
		recorder = null;
		lapInfo = null;
		replayBuffer = null;
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
		replayBuffer.reset();
	}

	public void setBestReplay( Replay replay ) {
		replayBuffer.setBestReplay( replay );
		lapInfo.setBestTrackTimeSeconds( replay.trackTimeSeconds );
	}

	// getters

	public TrackLapInfo getLapInfo() {
		return lapInfo;
	}

	/** Returns whether or not the Best or Worst replay is available */
	public boolean hasAnyReplay() {
		return replayBuffer.hasAnyReplayData();
	}

	/** Returns the first available, and valid, replay */
	public Replay getAnyReplay() {
		return replayBuffer.getAnyReplay();
	}

	/** Returns whether or not the Best and Worst replays are available */
	public boolean hasAllReplays() {
		return replayBuffer.hasAllReplayData();
	}

	/** Returns the best replay available, so far */
	public Replay getBestReplay() {
		return replayBuffer.getBestReplay();
	}

	/** Returns the worst replay available, so far */
	public Replay getWorstReplay() {
		return replayBuffer.getWorstReplay();
	}

	public Replay getLastRecordedReplay() {
		return lastRecordedReplay;
	}

	public boolean isLastBestLap() {
		return (lastRecordedReplay.id == replayBuffer.getBestReplay().id);
	}

	// triggered from game logic

	public void onPlayerComputeForces( CarForces forces ) {
		if( recorder.isRecording() ) {
			recorder.add( forces );
		}
	}

	/** Starts recording the player lap performance. */
	public void startRecording( PlayerCar player ) {
		if( recorder.isRecording() ) {
			Gdx.app.log( "TrackLapManager", "Couldn't start recording since it's already started." );
			return;
		}

		lapInfo.restartTime();
		Replay buffer = replayBuffer.getNextBuffer();
		recorder.beginRecording( player, buffer, gameWorld.levelName, settings.difficulty );
	}

	/** Ends recording the previously started lap performance */
	public void stopRecording() {
		if( recorder.isRecording() ) {
			// ends recording and keeps track of the last recorded replay
			lastRecordedReplay = recorder.endRecording();

			replayBuffer.updateReplays();

			// update lap info with last lap times
			if( replayBuffer.hasAllReplayData() ) {
				// lap finished, update lapinfo with the last recorded replay
				lapInfo.setLastTrackTimeSeconds( lastRecordedReplay.trackTimeSeconds );

			} else {
				// lap finished, update lapinfo with whatever replay data is available
				lapInfo.setLastTrackTimeSeconds( replayBuffer.getAnyReplay().trackTimeSeconds );
			}

			// update lap info with best lap time
			lapInfo.setBestTrackTimeSeconds( replayBuffer.getBestReplay().trackTimeSeconds );
		}
	}
}
