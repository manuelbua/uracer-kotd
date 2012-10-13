
package com.bitfire.uracer.game.logic.types.common;

import com.badlogic.gdx.Gdx;
import com.bitfire.uracer.game.Time;
import com.bitfire.uracer.game.Time.Reference;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.logic.helpers.GameTrack;

public class WrongWayMonitor {

	private final Time wrongWayTimer = new Time();
	private boolean isWrongWay = false;
	private WrongWayMonitorListener listener;
	private Car car;
	private GameTrack gameTrack;

	public static interface WrongWayMonitorListener {
		void onWrongWayBegins ();

		void onWrongWayEnds ();
	}

	public WrongWayMonitor (WrongWayMonitorListener listener, GameTrack gameTrack) {
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
		isWrongWay = false;
		wrongWayTimer.reset();
	}

	public void update () {
		if (car == null) {
			return;
		}

		float confidence = gameTrack.getTrackRouteConfidence(car);
		if (confidence <= 0) {

			// player wrong

			// if not already marked as on the wrong way, check if confidence stays negative for more than <n> seconds
			// after staying positive, then it's safe to assume the wrong way is being played

			if (!isWrongWay) {
				if (wrongWayTimer.isStopped()) {
					Gdx.app.log("CommonLogic", "--> detect wrong way begin");
					wrongWayTimer.start();
				} else if (wrongWayTimer.elapsed(Reference.TickSeconds) > 0.5f) {
					wrongWayTimer.reset();

					isWrongWay = true;
					listener.onWrongWayBegins();
				}
			} else {
				// player changed his mind earlier and there weren't enough seconds of wrong way to mark it
				// as that, reset the timer
				if (!wrongWayTimer.isStopped()) {
					Gdx.app.log("CommonLogic", "Player changed his mind earlier, resetting the wrong way timer (begin detector)");
					wrongWayTimer.reset();
				}
			}

		} else if (confidence > 0) {

			// player correct

			// if not already marked as on the good way, check if confidence stays positive for more than <n> seconds
			// after staying negative, then it's safe to assume the right way is being played

			if (isWrongWay) {
				if (wrongWayTimer.isStopped()) {
					wrongWayTimer.start();
					Gdx.app.log("CommonLogic", "<-- detect wrong way end");
				} else if (wrongWayTimer.elapsed(Reference.TickSeconds) > 1) {
					// wrong way finished

					wrongWayTimer.reset();
					isWrongWay = false;
					listener.onWrongWayEnds();
				}
			} else {
				// player changed his mind earlier and there weren't enough seconds of wrong way to mark it
				// as that, reset the timer
				if (!wrongWayTimer.isStopped()) {
					Gdx.app.log("CommonLogic", "Player changed his mind earlier, resetting the wrong way timer (end detector)");
					wrongWayTimer.reset();
				}
			}

		}
	}

	public boolean isWrongWay () {
		return isWrongWay;
	}
}
