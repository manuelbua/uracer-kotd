
package com.bitfire.uracer.game.logic.replaying;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.game.GameplaySettings;

/** Maintains an updated list of the best <n> Replay objects for the specified track level */
public final class ReplayManager implements Disposable {

	public static final int MaxReplays = 10;
	private final String trackId;
	private final Array<Replay> nreplays = new Array<Replay>();
	private final ReplayInfo replayInfo = new ReplayInfo();

	public enum DiscardReason {
		Null, InvalidMinDuration, Invalid, WrongTrack, Slower, NotDiscarded
	}

	/** Describes Replay position and state */
	public static final class ReplayInfo {
		public int position;
		public boolean accepted;
		public Replay replay;
		public DiscardReason reason;
		public Replay removed;

		public void reset () {
			position = -1;
			accepted = false;
			replay = null;
			removed = null;
			reason = DiscardReason.Null;
		}
	}

	public ReplayManager (String currentTrackId) {
		trackId = currentTrackId;
	}

	@Override
	public void dispose () {
		for (Replay r : nreplays) {
			r.dispose();
		}
	}

	private boolean isValidReplay (Replay replay, ReplayInfo outInfo) {
		// in case its invalid, returns the original Replay instance
		outInfo.accepted = false;
		outInfo.replay = replay;

		if (replay == null) {
			outInfo.reason = DiscardReason.Null;
			return false;
		}

		if (replay.getTrackTime() < GameplaySettings.ReplayMinDurationSecs) {
			outInfo.reason = DiscardReason.InvalidMinDuration;
			return false;
		}

		if (!replay.isValid()) {
			outInfo.reason = DiscardReason.Invalid;
			return false;
		}

		if (!replay.getTrackId().equals(trackId)) {
			outInfo.reason = DiscardReason.WrongTrack;
			return false;
		}

		return true;
	}

	/** Add a Replay to the list: it will get checked against some rules before adding it, so it's safe to assume that any Replay
	 * returned later will be valid.
	 * 
	 * @param replay The Replay to add
	 * @param computeId Whether to compute an ID for the specified Replay if added successfully
	 * @return A ReplayInfo structure describing the inserting position or failure explanation */
	public ReplayInfo addReplay (Replay replay) {
		replayInfo.reset();

		if (isValidReplay(replay, replayInfo)) {
			Replay added = new Replay();
			added.copyData(replay);
			nreplays.add(added);
			nreplays.sort();

			// specified Replay has been copied to a new instance, use this instead
			replayInfo.replay = added;

			if (nreplays.size > MaxReplays) {
				replayInfo.removed = nreplays.pop();
			}

			int pos = nreplays.indexOf(added, true);
			if (pos > -1) {
				// replay accepted
				replayInfo.accepted = true;
				replayInfo.reason = DiscardReason.NotDiscarded;
				replayInfo.position = pos + 1;
			} else {
				// replay discarded
				replayInfo.accepted = false;
				replayInfo.reason = DiscardReason.Slower;
			}
		}

		return replayInfo;
	}

	public void removeAll () {
		for (Replay r : nreplays) {
			r.reset();
		}

		nreplays.clear();
	}

	public boolean hasReplays () {
		return nreplays.size > 0;
	}

	public Replay getBestReplay () {
		return nreplays.first();
	}

	public Replay getWorstReplay () {
		return nreplays.peek();
	}

	public Array<Replay> getReplays () {
		return nreplays;
	}
}
