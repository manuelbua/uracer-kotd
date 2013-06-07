
package com.bitfire.uracer.game.logic.helpers;

import com.bitfire.uracer.game.actors.CarPreset;
import com.bitfire.uracer.game.actors.CarPreset.Type;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.world.GameWorld;

public final class CarFactory {
	private CarFactory () {
	}

	public static PlayerCar createPlayer (GameWorld gameWorld) {
		return new PlayerCar(gameWorld, Type.L2_PinkBeast);
	}

	public static GhostCar createGhost (int id, GameWorld gameWorld, CarPreset.Type presetType) {
		return new GhostCar(id, gameWorld, Type.L2_PinkBeast);
	}
}
