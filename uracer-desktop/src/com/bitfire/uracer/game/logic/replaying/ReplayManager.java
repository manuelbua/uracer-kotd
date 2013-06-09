
package com.bitfire.uracer.game.logic.replaying;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.uracer.configuration.UserProfile;
import com.bitfire.uracer.game.GameplaySettings;
import com.bitfire.utils.ItemsManager;

/** Maintains an updated list of the best <n> Replay objects for the specified track level */
public final class ReplayManager implements Disposable {

	public static final int MaxReplays = 2;
	private final String trackId;
	private final ItemsManager<Replay> replays = new ItemsManager<Replay>();

	private Replay best, worst;
	private int ridx;

	public ReplayManager (UserProfile userProfile, String trackId) {
		this.trackId = trackId;
		for (int i = 0; i < MaxReplays; i++) {
			replays.add(new Replay(userProfile.userId));
		}

		best = null;
		worst = null;
		ridx = 0;
	}

	@Override
	public void dispose () {
		replays.dispose();
	}

	public Replay addReplay (Replay replay) {
		if (replay == null) {
			Gdx.app.log("ReplayManager", "Discarding null replay");
			return null;
		}

		if (replay.getTrackTime() < GameplaySettings.ReplayMinDurationSecs) {
			Gdx.app.log("ReplayManager", "Invalid lap detected, (" + replay.getTrackTime() + "sec < "
				+ GameplaySettings.ReplayMinDurationSecs + ")");
			return null;
		}

		if (!replay.isValid()) {
			Gdx.app.log("ReplayManager", "The specified replay is not valid.");
			return null;
		}

		if (!replay.getTrackId().equals(trackId)) {
			Gdx.app.log("ReplayManager", "The specified replay belongs to another game track.");
			return null;
		}

		Replay added = null;

		// empty?
		if (ridx == 0) {
			added = replays.get(ridx++);
			added.copyData(replay);

			// update state
			best = added;
			worst = added;
		} else {

			if (replay == worst) {
				Gdx.app.log("!!!!", "!!");
			}

			if (replay.getTrackTime() >= worst.getTrackTime()) {
				Gdx.app.log("ReplayManager",
					"Discarded, worse than the worst! (" + replay.getTrackTime() + " >= " + worst.getTrackTime() + ")");
				return null;
			}

			if (ridx == MaxReplays) {
				// full, overwrite worst
				worst.copyData(replay);
				added = worst;

				// recompute best/worst
				worst = replays.get(0);
				best = replays.get(0);
				for (int i = 1; i < MaxReplays; i++) {
					Replay r = replays.get(i);

					if (worst.getTrackTime() < r.getTrackTime()) {
						worst = r;
					}

					if (best.getTrackTime() > r.getTrackTime()) {
						best = r;
					}
				}
			} else {
				// add new
				added = replays.get(ridx++);
				added.copyData(replay);

				// compute best
				if (best.getTrackTime() > added.getTrackTime()) {
					best = added;
				}
			}
		}

		// dump replays
		// for (int i = 0; i < MaxReplays; i++) {
		// Replay r = replays.items.get(i);
		// if (r.isValid) {
		// Gdx.app.log("ReplayManager", "#" + i + ", seconds=" + r.trackTimeSeconds);
		// }
		// }

		if (added != null) {
			Gdx.app.log("ReplayManager", "added!");
		}
		return added;
	}

	public void reset () {
		ridx = 0;
		for (int i = 0; i < MaxReplays; i++) {
			replays.get(i).reset();
		}
	}

	public boolean hasReplays () {
		return replays.count() > 0;
	}

	public boolean canClassify () {
		return (best != worst && best != null && worst != null && best.isValid() && worst.isValid());
	}

	public Replay getBestReplay () {
		return best;
	}

	public Replay getWorstReplay () {
		return worst;
	}

	public Iterable<Replay> getReplays () {
		return replays;
	}
}
