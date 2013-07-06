
package com.bitfire.uracer.game.debug;

public final class RankInfo implements Comparable<RankInfo> {
	public float completion;
	public String uid;
	public float secs;
	public boolean valid, player, isNextTarget;

	@Override
	public int compareTo (RankInfo o) {
		if (!valid) return 1;

		if (completion == 0) {
			if (player) return 1; // player stay at bottom

			// order by time
			if (secs < o.secs) return -1;
			if (secs > o.secs) return 1;
			return 0;
		}

		if (completion > o.completion) return -1;
		if (completion < o.completion) return 1;
		return 0;
	}
}
