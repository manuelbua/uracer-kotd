
package com.bitfire.uracer.game.logic.helpers;

import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.InterpolatedFloat;

public class TrackProgressData {
	private static final float Smoothing = 0.25f;

	public float playerToTarget;
	public boolean isCurrentLapValid, isWarmUp;
	public InterpolatedFloat playerDistance, targetDistance;
	public InterpolatedFloat playerProgress, playerProgressAdv, targetProgress;

	public TrackProgressData () {
		playerDistance = new InterpolatedFloat();
		targetDistance = new InterpolatedFloat();
		playerProgress = new InterpolatedFloat();
		playerProgressAdv = new InterpolatedFloat();
		targetProgress = new InterpolatedFloat();
		reset(true);
	}

	public void reset (boolean resetState) {
		isCurrentLapValid = true;
		isWarmUp = true;
		playerToTarget = 0;

		playerDistance.reset(0, resetState);
		targetDistance.reset(0, resetState);
		playerProgress.reset(0, resetState);
		playerProgressAdv.reset(0, resetState);
		targetProgress.reset(0, resetState);
	}

	public void update (boolean isWarmUp, boolean isCurrentLapValid, GameTrack gameTrack, PlayerCar player, GhostCar target) {
		this.isCurrentLapValid = isCurrentLapValid;
		this.isWarmUp = isWarmUp;
		boolean hasTarget = (target != null /* && target.getTrackState().ghostStarted */);

		if (isCurrentLapValid) {
			playerToTarget = 0;
		} else {
			playerToTarget = -1;
		}

		if (isWarmUp) {
			reset(true);
		} else {
			if (isCurrentLapValid) {
				playerProgress.set(gameTrack.getTrackCompletion(player), Smoothing);
				playerDistance.set(gameTrack.getTrackDistance(player, 0), Smoothing);

				if (hasTarget) {
					playerProgressAdv.set(gameTrack.getTrackCompletion(player), Smoothing);
					if (target.getTrackState().ghostArrived) {
						playerToTarget = AMath.fixup(playerProgressAdv.get() - 1);
					} else {
						targetDistance.set(gameTrack.getTrackDistance(target, 0), Smoothing);
						targetProgress.set(gameTrack.getTrackCompletion(target), Smoothing);
						playerToTarget = AMath.fixup(playerProgressAdv.get() - targetProgress.get());
					}
				}
			} else {
				reset(true);
			}
		}
	}
}
