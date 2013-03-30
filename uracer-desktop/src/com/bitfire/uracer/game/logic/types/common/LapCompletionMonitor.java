
package com.bitfire.uracer.game.logic.types.common;

import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.logic.helpers.GameTrack;

public class LapCompletionMonitor {
	private GameTrack gameTrack;
	private Car car;
	private LapCompletionMonitorListener listener;
	private float previousCompletion, currentCompletion, wuStart, wuPrev, wuCurr, wuCompletion;

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
		reset();

		if (car != null) {
			wuStart = gameTrack.getTrackCompletion(car);
		}
	}

	public void reset () {
		previousCompletion = 0;
		currentCompletion = -1;
		wuPrev = wuCurr = wuStart = wuCompletion = 0;
	}

	public float getWarmUpCompletion () {
		return wuCompletion;
	}

	public void update (boolean isWarmUp) {
		if (car != null) {

			if (isWarmUp) {
				// compute warmup quantity (1 at warmup start towards 0 at the start line)
				wuPrev = wuCurr;
				wuCurr = (gameTrack.getTrackCompletion(car) - wuStart) / (1 - wuStart);
				wuCompletion = wuCurr;

				if ((wuPrev > 0 && wuCurr < 0)) {
					// warmup will ends
					wuCompletion = 1;
					listener.onLapStarted();
				}

				// Gdx.app.log("LapCompletionMonitor", "wucompletion=" + wuCompletion);

			} else {
				previousCompletion = currentCompletion;
				currentCompletion = gameTrack.getTrackCompletion(car);
				// Gdx.app.log("", "curr=" + currentCompletion + ", prev=" + previousCompletion);
				if (previousCompletion > 0.9f && currentCompletion >= 0 && currentCompletion < 0.1f) {
					// reset();
					listener.onLapComplete();
					listener.onLapStarted();
				}
				// else {
				// previousCompletion = currentCompletion;
				// }
			}
		}
	}
}
