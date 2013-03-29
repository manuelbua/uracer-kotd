
package com.bitfire.uracer.game.logic.types.common;

import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.logic.helpers.GameTrack;

public class LapCompletionMonitor {
	private GameTrack gameTrack;
	private Car car;
	private LapCompletionMonitorListener listener;
	private float previousCompletion, currentCompletion;
	private boolean isLapStarted, isLapCompleted;

	public static interface LapCompletionMonitorListener {
		void onLapStarted ();

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
		isLapStarted = false;
		isLapCompleted = false;
	}

	public void update () {
		if (car != null) {
			currentCompletion = gameTrack.getTrackCompletion(car, 0);
// Gdx.app.log("", "curr=" + currentCompletion + ", prev=" + previousCompletion);

			if (!isLapStarted && (previousCompletion <= 0 && currentCompletion > 0f)) {
				listener.onLapStarted();
				isLapStarted = true;
				isLapCompleted = false;
			} else if (!isLapCompleted && (previousCompletion > 0.9f && currentCompletion >= 0 && currentCompletion < 0.1f)) {
				reset();
				listener.onLapComplete();
				listener.onLapStarted();
				isLapStarted = false;
				isLapCompleted = true;
			} else {
				previousCompletion = currentCompletion;
			}
		}
	}
}
