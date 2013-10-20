
package com.bitfire.uracer.game.logic.types.helpers;

import com.badlogic.gdx.math.MathUtils;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.events.PlayerLapCompletionMonitorEvent;
import com.bitfire.uracer.game.logic.helpers.GameTrack;
import com.bitfire.uracer.game.logic.helpers.GameTrack.TrackState;

public class PlayerLapCompletionMonitor {
	protected GameTrack gameTrack;
	protected float prev, completion, wuPrev, wuCompletion;
	private boolean warmUpStartedCalled, isWarmUp;

	public PlayerLapCompletionMonitor (GameTrack gameTrack) {
		this.gameTrack = gameTrack;
		reset(true);
	}

	public void reset () {
		reset(true);
	}

	protected void reset (boolean warmUp) {
		prev = 0;
		completion = -1;
		wuPrev = wuCompletion = 0;
		warmUpStartedCalled = false;
		isWarmUp = warmUp;
	}

	public float getWarmUpCompletion () {
		return wuCompletion;
	}

	public boolean isWarmUp () {
		return isWarmUp;
	}

	public void update (Car car) {
		if (car != null) {
			TrackState state = car.getTrackState();

			if (isWarmUp) {
				if (!warmUpStartedCalled) {
					warmUpStartedCalled = true;
					GameEvents.lapCompletion.trigger(car, PlayerLapCompletionMonitorEvent.Type.onWarmUpStarted);
				}

				// compute warmup quantity (0 at WU start pos, 1 at WU end pos)
				wuPrev = MathUtils.clamp(wuCompletion, 0, 1);
				float complet = gameTrack.getTrackCompletion(car);
				float start = state.initialCompletion;
				wuCompletion = MathUtils.clamp((complet - start) / (1 - start), 0, 1);

				// Gdx.app.log("LapCompletionMonitor", "wc=" + wuCompletion + ", wp=" + wuPrev);

				if (hasFinished(wuPrev, wuCompletion)) {
					wuCompletion = 1;
					isWarmUp = false;
					GameEvents.lapCompletion.trigger(car, PlayerLapCompletionMonitorEvent.Type.onWarmUpCompleted);
					GameEvents.lapCompletion.trigger(car, PlayerLapCompletionMonitorEvent.Type.onLapStarted);
				}

			} else {
				prev = completion;
				completion = gameTrack.getTrackCompletion(car);
				// Gdx.app.log("LapCompletionMonitor", "c=" + completion + ", p=" + prev);
				if (hasFinished(prev, completion)) {
					GameEvents.lapCompletion.trigger(car, PlayerLapCompletionMonitorEvent.Type.onLapCompleted);
					GameEvents.lapCompletion.trigger(car, PlayerLapCompletionMonitorEvent.Type.onLapStarted);
				}
			}
		}
	}

	protected boolean hasFinished (float prev, float curr) {
		return (prev > 0.9f && curr >= 0 && curr < 0.1f);
	}
}
