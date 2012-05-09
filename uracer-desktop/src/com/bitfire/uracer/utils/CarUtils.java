package com.bitfire.uracer.utils;

import com.badlogic.gdx.Gdx;
import com.bitfire.uracer.game.actors.Car;

public final class CarUtils {

	private CarUtils() {
	}

	public static float mtSecToKmHour( float mtsec ) {
		return mtsec * 3.6f;
	}

	public static void dumpSpeedInfo( String msg, Car car, float timeElapsed ) {
		// @formatter:off

		float  dist = car.getTraveledDistance();
		float mts = dist / timeElapsed;
//		float kmh = mtSecToKmHour(mts);

		Gdx.app.log( msg,
			"Car traveled " + dist + " m " +
			"(" + car.getAccuDistCount() + ") " + "in " +
			timeElapsed + " s " +
			"(" + mts + " mt/s) " +
//			"(" + kmh + " km/h) " +
//			"(" + car.getAccuSpeedCount() + ")" +
//			"[" + AMath.round( kmh, 2 ) + " km/h, " +
//			AMath.round( dist, 2 ) + " m]" +
			" fpos=" + car.getBody().getPosition()
		);

		// @formatter:on
	}
}
