
package com.bitfire.uracer.game.logic.types.helpers;

import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.GameplaySettings;
import com.bitfire.uracer.game.Time;
import com.bitfire.uracer.game.events.WrongWayMonitorEvent;

public class WrongWayMonitor {

	private final Time wrongWayTimer = new Time();
	private boolean isWrongWay = false;

	public WrongWayMonitor () {
		reset();
	}

	public void reset () {
		isWrongWay = false;
		wrongWayTimer.reset();
	}

	public void update (float trackRouteConfidence) {
		if (trackRouteConfidence <= 0) {

			// player wrong

			// if not already marked as on the wrong way, check if confidence stays negative for more than <n> seconds
			// after staying positive, then it's safe to assume the wrong way is being played

			if (!isWrongWay) {
				// try mark wrong way
				if (wrongWayTimer.isStopped()) {
					// Gdx.app.log("WrongWayMonitor", "--> detect wrong way begins");
					wrongWayTimer.start();
				} else if (wrongWayTimer.elapsed().tickSeconds > GameplaySettings.MaxSecondsWrongWayDetector) {
					wrongWayTimer.reset();
					isWrongWay = true;
					GameEvents.wrongWay.trigger(null, WrongWayMonitorEvent.Type.onWrongWayBegins);
					// Gdx.app.log("WrongWayMonitor", "--> wrong way detected, invalidating lap");
				} else {
					// Gdx.app.log("WrongWayMonitor", "--> " + wrongWayTimer.elapsed().tickSeconds);
				}
			}

		} else if (trackRouteConfidence > 0 && !isWrongWay) {

			// player correct

			// player changed his mind earlier and there weren't enough seconds of wrong way to mark it
			// as that, reset the timer
			if (!wrongWayTimer.isStopped()) {
				// Gdx.app.log("WrongWayMonitor", "<-- player got it right in time, wrong way detection ends");
				wrongWayTimer.reset();
			}
		}
	}

	public boolean isWrongWay () {
		return isWrongWay;
	}
}
