
package com.bitfire.uracer.game.debug;

public final class RankInfo implements Comparable<RankInfo> {
	public float completion;
	public String uid;
	public int ticks;
	public boolean valid, player, isNextTarget;

	public RankInfo () {
		reset();
	}

	public void reset () {
		completion = 0;
		uid = "";
		ticks = 0;
		valid = false;
		player = false;
		isNextTarget = false;
	}

	// @Override
	// public boolean equals (Object obj) {
	// return this.compareTo((RankInfo)obj) == 0;
	// }

	@Override
	public int compareTo (RankInfo o) {
		if (o == null) {
			throw new NullPointerException();
		}

		if (!valid) return 1;
		if (!o.valid) return -1;

		if (completion == 0 && player) return 1; // player at bottom if in warmup
		if (o.completion == 0 && o.player) return -1; // player at bottom if in warmup

		if (completion == 0 || o.completion == 0) {
			// order by time
			if (ticks < o.ticks) return -1;
			if (ticks > o.ticks) return 1;
		} else {

			if (completion > o.completion) return -1;
			if (completion < o.completion) return 1;
		}

		return 0;
	}
}
