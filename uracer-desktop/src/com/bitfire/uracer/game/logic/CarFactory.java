package com.bitfire.uracer.game.logic;

import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.Car.Aspect;
import com.bitfire.uracer.game.actors.CarModel;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.world.GameWorld;

public final class CarFactory {
	private CarFactory() {
	}

	public static GhostCar createGhost( GameWorld gameWorld, CarModel model, Aspect type ) {
		return new GhostCar( gameWorld, model, type );
	}

	public static GhostCar createGhost( GameWorld gameWorld, Car car ) {
		return CarFactory.createGhost( gameWorld, car.getCarModel(), car.getAspect() );
	}

	public static PlayerCar createPlayer( GameWorld gameWorld, CarModel model, Aspect carAspect ) {
		return new PlayerCar( gameWorld, model, carAspect );
	}
}
