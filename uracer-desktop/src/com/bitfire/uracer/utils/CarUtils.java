
package com.bitfire.uracer.utils;

import com.badlogic.gdx.Gdx;
import com.bitfire.uracer.game.actors.Car;

public final class CarUtils {

	private CarUtils () {
	}

	public static float mtSecToKmHour (float mtsec) {
		return mtsec * 3.6f;
	}

	public static void dumpSpeedInfo (String msg, String subject, Car car, int ticks) {

		float elapsed = ReplayUtils.ticksToSeconds(ticks);
		float dist = car.getTraveledDistance();
		float mts = dist / elapsed;
		// float kmh = mtSecToKmHour(mts);

		// @off
		Gdx.app.log(msg, subject + " traveled " + dist + "m " + "(" + car.getAccuDistCount() + ") " + "in " + String.format("%.03f", elapsed) + "s " + "("
			+ mts + "mt/s) " +
			// "(" + kmh + " km/h) " +
			// "(" + car.getAccuSpeedCount() + ")" +
			// "[" + AMath.round( kmh, 2 ) + " km/h, " +
			// AMath.round( dist, 2 ) + " m]" +
			" fpos=" + car.getBody().getPosition());

		// @on
	}
}
