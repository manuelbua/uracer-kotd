
package com.bitfire.uracer.game.logic.types.helpers;

import com.badlogic.gdx.math.MathUtils;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.logic.helpers.GameTrack;

public final class LapCompletionMonitor {
	private GameTrack gameTrack;
	private Car car;
	private LapCompletionMonitorListener listener;
	private float previousCompletion, currentCompletion, wuStart, wuPrev, wuCurr, wuCompletion;
	private boolean warmUpStartedCalled, isWarmUp;

	public static interface LapCompletionMonitorListener {
		void onWarmUpStarted ();

		void onWarmUpCompleted ();

		void onLapStarted ();

		void onLapCompleted ();
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
		warmUpStartedCalled = false;
		isWarmUp = true;
	}

	public float getWarmUpCompletion () {
		return wuCompletion;
	}

	public boolean isWarmUp () {
		return isWarmUp;
	}

	public void update () {
		if (car != null) {
			if (isWarmUp) {

				if (!warmUpStartedCalled) {
					warmUpStartedCalled = true;
					listener.onWarmUpStarted();
				}

				// compute warmup quantity (0 at WU start pos, 0 at WU end pos)
				wuPrev = MathUtils.clamp(wuCurr, 0, 1);
				float complet = gameTrack.getTrackCompletion(car);
				wuCurr = (complet - wuStart) / (1 - wuStart);

				if (wuPrev > 0.9f && wuCurr >= 0 && wuCurr < 0.1f) {
					// warmup will ends
					wuCompletion = 1;
					isWarmUp = false;

					listener.onWarmUpCompleted();
					listener.onLapStarted();
				} else {
					wuCompletion = MathUtils.clamp(wuCurr, 0, 1);
				}

				// Gdx.app.log("LapCompletionMonitor", "wucompletion=" + complet);
				// Gdx.app.log("LapCompletionMonitor", "wc=" + wuCurr + ", wp=" + wuPrev);
				// TrackState ts = gameTrack.getTrackState(car);
				// Gdx.app.log("LapCompletionMonitor", "ts=" + ts.initialCompletion + ", wp=" + wuPrev + ", wc=" + wuCurr);

			} else {
				previousCompletion = currentCompletion;
				currentCompletion = gameTrack.getTrackCompletion(car);
				// Gdx.app.log("", "curr=" + currentCompletion + ", prev=" + previousCompletion);
				if (previousCompletion > 0.9f && currentCompletion >= 0 && currentCompletion < 0.1f) {
					listener.onLapCompleted();
					listener.onLapStarted();
				}
			}
		}
	}
}
