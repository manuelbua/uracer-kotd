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

		float kmh = AMath.round( mtSecToKmHour(car.getAverageSpeed()), 3 );
		float dist = AMath.round( car.getTraveledDistance(), 3);

		Gdx.app.log( msg,
			"Car traveled " + dist + " mt (" + car.getAccuDistCount() + ") " +
			"in "+ timeElapsed + "s (" + kmh + "km/h) " + "(" + car.getAccuSpeedCount() + ")"
		);

		// @formatter:on
	}
}
