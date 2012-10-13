
package com.bitfire.uracer.game.logic.types.common;

import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.logic.helpers.GameTrack;

public class LapCompletionMonitor {
	private GameTrack gameTrack;
	private Car car;
	private LapCompletionMonitorListener listener;
	private float previousCompletion, currentCompletion;

	public static interface LapCompletionMonitorListener {
		void onLapComplete ();
	}

	public LapCompletionMonitor (LapCompletionMonitorListener listener, GameTrack gameTrack) {
		this.listener = listener;
		this.gameTrack = gameTrack;
		this.car = null;
		reset();
	}

	public void setCar (Car car) {
		this.car = car;
		if (car == null) {
			reset();
		}
	}

	public void reset () {
		previousCompletion = 0;
		currentCompletion = -1;
	}

	public void update () {
		currentCompletion = gameTrack.getTrackCompletion(car, 0);
		// Gdx.app.log("", "curr=" + currentCompletion + ", prev=" + previousCompletion);

		if (previousCompletion > 0.9f && currentCompletion >= 0 && currentCompletion < 0.1f) {
			reset();
			listener.onLapComplete();
		} else {
			previousCompletion = currentCompletion;
		}
	}
}
