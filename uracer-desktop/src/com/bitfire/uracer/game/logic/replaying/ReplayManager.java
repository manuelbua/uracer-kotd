
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
		public Replay discarded;

		public void reset () {
			position = -1;
			accepted = false;
			replay = null;
			discarded = null;
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
	 * The policy is to permit slower replays at loading time, but not at gameplay time, so that a player will not be able to save
	 * a slower replay, but if it could, it would be loaded fine from disk.
	 * 
	 * @param replay The Replay to add
	 * @param computeId Whether to compute an ID for the specified Replay if added successfully
	 * @return A ReplayInfo structure describing the inserting position or failure explanation */
	public ReplayInfo addReplay (Replay replay, boolean evenIfSlower) {
		replayInfo.reset();

		if (isValidReplay(replay, replayInfo)) {
			Replay new_replay = new Replay();
			replayInfo.replay = new_replay;
			new_replay.copyData(replay);

			// filter slowers out only if asked to
			if (!evenIfSlower) {
				// a new replay is added only if it's the first one or better than the actual best
				if (nreplays.size > 0 && replay.compareTo(nreplays.first()) > -1) {
					// replay discarded, slower
					replayInfo.accepted = false;
					replayInfo.discarded = new_replay;
					replayInfo.reason = DiscardReason.Slower;
					return replayInfo;
				}
			}

			nreplays.add(new_replay);
			nreplays.sort();

			if (nreplays.size > MaxReplays) {
				// a replay has been removed
				replayInfo.discarded = nreplays.pop();
			}

			int pos = nreplays.indexOf(new_replay, true);
			if (pos > -1) {
				// replay accepted
				replayInfo.accepted = true;
				replayInfo.reason = DiscardReason.NotDiscarded;
				replayInfo.position = pos + 1;
			} else {
				// // replay discarded, slower
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

	public int getReplaysCount () {
		return nreplays.size;
	}
}
