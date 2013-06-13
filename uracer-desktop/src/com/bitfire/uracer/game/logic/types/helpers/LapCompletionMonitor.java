
package com.bitfire.uracer.game.logic.types.helpers;

import com.badlogic.gdx.math.MathUtils;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.events.LapCompletionMonitorEvent;
import com.bitfire.uracer.game.logic.helpers.GameTrack;

public final class LapCompletionMonitor {
	private GameTrack gameTrack;
	private Car car;
	private float previousCompletion, currentCompletion, wuStart, wuPrev, wuCurr, wuCompletion;
	private boolean warmUpStartedCalled, isWarmUp;

	public LapCompletionMonitor (GameTrack gameTrack) {
		this.gameTrack = gameTrack;
		this.car = null;
		reset();
	}

	public void reset () {
		reset(null);
	}

	public void reset (Car car) {
		this.car = car;
		previousCompletion = 0;
		currentCompletion = -1;
		wuPrev = wuCurr = wuStart = wuCompletion = 0;
		warmUpStartedCalled = false;
		isWarmUp = true;

		if (car != null) {
			wuStart = gameTrack.getTrackCompletion(car);
		}
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
					GameEvents.lapCompletion.trigger(null, LapCompletionMonitorEvent.Type.onWarmUpStarted);
				}

				// compute warmup quantity (0 at WU start pos, 0 at WU end pos)
				wuPrev = MathUtils.clamp(wuCurr, 0, 1);
				float complet = gameTrack.getTrackCompletion(car);
				wuCurr = (complet - wuStart) / (1 - wuStart);

				if (hasFinished(wuPrev, wuCurr)) {
					// warmup will ends
					wuCompletion = 1;
					isWarmUp = false;

					GameEvents.lapCompletion.trigger(null, LapCompletionMonitorEvent.Type.onWarmUpCompleted);
					GameEvents.lapCompletion.trigger(null, LapCompletionMonitorEvent.Type.onLapStarted);
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
				if (hasFinished(previousCompletion, currentCompletion)) {
					GameEvents.lapCompletion.trigger(null, LapCompletionMonitorEvent.Type.onLapCompleted);
					GameEvents.lapCompletion.trigger(null, LapCompletionMonitorEvent.Type.onLapStarted);
				}
			}
		}
	}

	private boolean hasFinished (float prev, float curr) {
		return (prev > 0.9f && curr >= 0 && curr < 0.1f);
	}
}
