
package com.bitfire.uracer.game.logic.types.helpers;

import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.events.GhostLapCompletionMonitorEvent;
import com.bitfire.uracer.game.logic.helpers.GameTrack;
import com.bitfire.uracer.game.logic.helpers.GameTrack.TrackState;

public class GhostLapCompletionMonitor extends PlayerLapCompletionMonitor {

	public GhostLapCompletionMonitor (GameTrack gameTrack) {
		super(gameTrack);
	}

	@Override
	public boolean isWarmUp () {
		return false;
	}

	@Override
	public void reset () {
		super.reset(false);
	}

	@Override
	public void update (Car car) {
		if (car != null) {
			TrackState state = car.getTrackState();

			prev = completion;
			completion = gameTrack.getTrackCompletion(car);
			if (hasFinished(prev, completion)) {
				state.ghostArrived = true;
				GameEvents.ghostLapCompletion.trigger(car, GhostLapCompletionMonitorEvent.Type.onLapCompleted);
			}
		}
	}
}
