package com.bitfire.uracer.utils;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.bitfire.uracer.entities.EntityType;

public class Box2DUtils {
	public static boolean isPlayerAgainstGhost( Fixture fixtureA, Fixture fixtureB ) {
		if( isCar( fixtureA ) && isGhostCar( fixtureB ) || isCar( fixtureB ) && isGhostCar( fixtureA ) ) {
			return true;
		}

		return false;
	}

	public static boolean isCar( Fixture fixture ) {
		return fixture.getUserData() == EntityType.Car;
	}

	public static boolean isGhostCar( Fixture fixture ) {
		return fixture.getUserData() == EntityType.CarReplay;
	}
}
