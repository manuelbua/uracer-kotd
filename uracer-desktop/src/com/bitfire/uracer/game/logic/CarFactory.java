package com.bitfire.uracer.game.logic;

import com.bitfire.uracer.game.actors.CarPreset;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.world.GameWorld;

public final class CarFactory {
	private CarFactory() {
	}

	public static PlayerCar createPlayer( GameWorld gameWorld, CarPreset.Type presetType ) {
		return new PlayerCar( gameWorld, presetType );
	}

	public static GhostCar createGhost( GameWorld gameWorld, CarPreset.Type presetType ) {
		return new GhostCar( gameWorld, presetType );
	}
}
