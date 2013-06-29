
package com.bitfire.uracer.game.logic.replaying;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.game.GameplaySettings;

/** Maintains an updated list of the best <n> Replay objects for the specified track level */
public final class ReplayManager implements Disposable {

	public static final int MaxReplays = 2;
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

			Gdx.app.log("ReplayManager", "Discarding null replay");
			return false;
		}

		if (replay.getTrackTime() < GameplaySettings.ReplayMinDurationSecs) {
			outInfo.reason = DiscardReason.InvalidMinDuration;

			Gdx.app.log("ReplayManager", "Invalid lap detected, (" + replay.getTrackTime() + "sec < "
				+ GameplaySettings.ReplayMinDurationSecs + ")");
			return false;
		}

		if (!replay.isValid()) {
			outInfo.reason = DiscardReason.Invalid;

			Gdx.app.log("ReplayManager", "The specified replay is not valid.");
			return false;
		}

		if (!replay.getTrackId().equals(trackId)) {
			outInfo.reason = DiscardReason.WrongTrack;

			Gdx.app.log("ReplayManager", "The specified replay belongs to another game track.");
			return false;
		}

		outInfo.accepted = true;
		outInfo.replay = null;
		return true;
	}

	public ReplayInfo addReplay (Replay replay) {
		replayInfo.reset();
		if (isValidReplay(replay, replayInfo)) {
			Replay added = new Replay();
			added.copyData(replay);
			nreplays.add(added);
			nreplays.sort();

			// specified Replay has been copied to a new instance, use it instead
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

				Gdx.app.log("ReplayManager", "--> at position #" + replayInfo.position);
			} else {
				// replay discarded
				replayInfo.accepted = false;
				replayInfo.reason = DiscardReason.Slower;

				Gdx.app.log("ReplayManager", "--> discarded (slower)");
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

	public Iterable<Replay> getReplays () {
		return nreplays;
	}
}
