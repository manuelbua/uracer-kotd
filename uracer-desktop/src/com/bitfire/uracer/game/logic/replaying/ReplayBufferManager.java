
package com.bitfire.uracer.game.logic.replaying;

/** TODO
 * 
 * @author bmanuel */
public final class ReplayBufferManager {
	// private final long userId;
	private Replay[] replays;
	private Replay best, worst;

	public ReplayBufferManager (long userId) {
		// this.userId = userId;

		// construct replay buffers
		replays = new Replay[2];
		replays[0] = new Replay(userId);
		replays[1] = new Replay(userId);

		best = null;
		worst = null;
		updateReplays();
	}

	public void reset () {
		best = null;
		worst = null;
		replays[0].reset();
		replays[1].reset();
	}

	private boolean hasAllReplayData () {
		return (replays[0].isValid() && replays[1].isValid());
	}

	private Replay getFirstValid () {
		if (replays[0].isValid()) {
			return replays[0];
		} else {
			return replays[1];
		}
	}

	public void updateReplays () {
		if (hasAllReplayData()) {
			best = replays[1];
			worst = replays[0];

			if (replays[0].getTrackTime() < replays[1].getTrackTime()) {
				best = replays[0];
				worst = replays[1];
			}
		} else {
			Replay r = getFirstValid();
			best = r;
		}
	}

	public Replay getNextBuffer () {
		updateReplays();
		if (!replays[0].isValid()) {
			return replays[0];
		}
		if (!replays[1].isValid()) {
			return replays[1];
		}

		// if both are valid
		return worst;
	}
}
