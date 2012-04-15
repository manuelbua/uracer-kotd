package com.bitfire.uracer.game.states;

import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.Replay;
import com.bitfire.uracer.game.Time;
import com.bitfire.uracer.game.events.GameLogicEvent;

public final class LapState {
	// replays
	private Replay[] replays;
	private Replay best, worst;
	private Time time;
	private long startTimeNs;
	private float lastTrackTimeSecs;
	private boolean hasLastTrackTimeSecs;

	private final GameLogicEvent.Listener gameLogicEvent = new GameLogicEvent.Listener() {
		@Override
		public void gameLogicEvent( GameLogicEvent.Type type ) {
			switch( type ) {
			case onReset:
				reset();
				break;
			}
		}
	};

	public LapState() {
		GameEvents.gameLogic.addListener( gameLogicEvent, GameLogicEvent.Type.onReset );
		GameEvents.gameLogic.addListener( gameLogicEvent, GameLogicEvent.Type.onRestart );

		startTimeNs = 0;
		lastTrackTimeSecs = 0;
		hasLastTrackTimeSecs = false;
		time = new Time();

		// construct replay buffers
		replays = new Replay[ 2 ];
		replays[0] = new Replay();
		replays[1] = new Replay();

		best = null;
		worst = null;

		reset();
		updateReplays();
	}

	public void reset() {
		hasLastTrackTimeSecs = false;
		best = null;
		worst = null;
		time.start();
		replays[0].reset();
		replays[1].reset();
		startTimeNs = System.nanoTime();
	}

	public long restart() {
		startTimeNs = System.nanoTime();
		time.start();
		if( !replays[0].isValid ) {
			replays[0].reset();
		}

		if( !replays[1].isValid ) {
			replays[1].reset();
		}

		return startTimeNs;
	}

	public float getElapsedSeconds() {
		// return ((float)(System.nanoTime() - startTimeNs) / 1000000000f) * URacer.timeMultiplier;
		return time.elapsed( Time.Reference.Ticks );
	}

	public Replay getReplay( int index ) {
		return replays[index];
	}

	public boolean hasAllReplayData() {
		return (replays[0].isValid && replays[1].isValid);
	}

	public boolean hasAnyReplayData() {
		return (replays[0].isValid || replays[1].isValid);
	}

	public void updateReplays() {
		if( !hasAllReplayData() ) {
			return;
		}

		best = replays[1];
		worst = replays[0];

		if( replays[0].trackTimeSeconds < replays[1].trackTimeSeconds ) {
			best = replays[0];
			worst = replays[1];
		}
	}

	public Replay getNextBuffer() {
		updateReplays();
		if( !replays[0].isValid ) {
			return replays[0];
		}
		if( !replays[1].isValid ) {
			return replays[1];
		}

		// if both are valid
		return getWorstReplay();
	}

	public Replay getBestReplay() {
		return best;
	}

	public Replay getWorstReplay() {
		return worst;
	}

	public void setLastTrackTimeSeconds( float value ) {
		lastTrackTimeSecs = value;
		hasLastTrackTimeSecs = true;
	}

	public float getLastTrackTimeSeconds() {
		return lastTrackTimeSecs;
	}

	public boolean hasLastTrackTimeSeconds() {
		return hasLastTrackTimeSecs;
	}

	public Replay getAnyReplay() {
		if( replays[0].isValid ) {
			return replays[0];
		}

		if( replays[1].isValid ) {
			return replays[1];
		}

		return null;
	}
}
