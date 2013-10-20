
package com.bitfire.uracer.game.actors;

public class CarTrackState {
	public int curr, next;
	public boolean onExpectedPath;

	public void reset () {
		curr = 0;
		next = 1;
		onExpectedPath = true;
	}
}
