
package com.bitfire.uracer.game.logic.replaying;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.game.GameplaySettings;

/** Maintains an updated list of the best <n> Replay objects for the specified track level */
public final class ReplayManager implements Disposable {

	public static final int MaxReplays = 5;
	private final String trackId;
	private final Array<Replay> nreplays = new Array<Replay>();
	private final ReplayResult replayInfo = new ReplayResult();

	public enum DiscardReason {
		Null, InvalidMinDuration, Invalid, WrongTrack, Slower, Accepted
	}

	/** Describes Replay position and state */
	public static final class ReplayResult {
		public int position;
		public boolean is_accepted;
		public DiscardReason reason;
		public ReplayInfo accepted;
		public ReplayInfo discarded;
		public ReplayInfo pruned;
		public Replay new_replay;

		public ReplayResult () {
			accepted = new ReplayInfo();
			discarded = new ReplayInfo();
			pruned = new ReplayInfo();
			new_replay = null;
			reset();
		}

		public void reset () {
			position = -1;
			is_accepted = false;
			reason = DiscardReason.Null;
			accepted.reset();
			discarded.reset();
			pruned.reset();
			new_replay = null;
		}

		public void copy (ReplayResult o) {
			this.is_accepted = o.is_accepted;
			this.accepted.copy(o.accepted);
			this.discarded.copy(o.discarded);
			this.pruned.copy(o.pruned);
			this.position = o.position;
			this.reason = o.reason;
			this.new_replay = o.new_replay; // shallow copy
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

	private boolean isValidReplay (Replay replay, ReplayResult outInfo) {
		// in case its invalid, returns the original Replay instance
		outInfo.is_accepted = false;
		outInfo.discarded.reset();

		if (replay == null) {
			outInfo.reason = DiscardReason.Null;
			return false;
		}

		outInfo.discarded.copy(replay.getInfo());

		if (!replay.info.isValid()) {
			outInfo.reason = DiscardReason.Invalid;
			return false;
		}

		if (replay.info.getTicks() < GameplaySettings.ReplayMinDurationTicks) {
			outInfo.reason = DiscardReason.InvalidMinDuration;
			return false;
		}

		if (!replay.info.getTrackId().equals(trackId)) {
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
	public ReplayResult addReplay (Replay replay) {
		replayInfo.reset();

		if (isValidReplay(replay, replayInfo)) {
			Replay new_replay = new Replay();
			new_replay.copy(replay);

			nreplays.add(new_replay);
			nreplays.sort();

			Replay removed = null;
			if (nreplays.size > MaxReplays) {
				removed = nreplays.pop();
			}

			int pos = nreplays.indexOf(new_replay, true);
			if (pos > -1) {
				// replay accepted
				replayInfo.is_accepted = true;
				replayInfo.accepted.copy(new_replay.info);
				replayInfo.reason = DiscardReason.Accepted;
				replayInfo.position = pos + 1;
				replayInfo.new_replay = new_replay;

				if (removed != null) {
					replayInfo.pruned.copy(removed.info);
				}
			} else {
				// replay discarded, slower
				replayInfo.is_accepted = false;
				replayInfo.reason = DiscardReason.Slower;
				if (removed != null) {
					replayInfo.discarded.copy(removed.info);
				}
			}
		}

		return replayInfo;
	}

	public Replay getById (String replayId) {
		for (Replay r : nreplays) {
			if (replayId.equals(r.info.getId())) {
				return r;
			}
		}

		return null;
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
