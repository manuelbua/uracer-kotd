package com.bitfire.uracer.game.data;

import com.bitfire.uracer.Art;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.carsimulation.CarModel;
import com.bitfire.uracer.game.GameDifficulty;
import com.bitfire.uracer.game.Tweener;
import com.bitfire.uracer.game.actors.CarAspect;
import com.bitfire.uracer.game.actors.CarFactory;
import com.bitfire.uracer.utils.BatchUtils;
import com.bitfire.uracer.utils.Convert;

/** Encapsulates and abstracts the dynamic state of the game.
 *
 * @author bmanuel */
public final class GameData {

	public static States States;
	public static Systems Systems;
	public static Environment Environment;

	// 1st
	public static void create( GameDifficulty difficulty ) {
		Environment = new Environment( difficulty );

		Tweener.init();
		Art.init( GameData.Environment.scalingStrategy.invTileMapZoomFactor );
		BatchUtils.init( Art.base6 );
		Convert.init( GameData.Environment.scalingStrategy.invTileMapZoomFactor, Config.Physics.PixelsPerMeter );
		Director.init();
	}

	// 2nd
	public static void createStates( CarAspect carType, CarModel carModel ) {
		States = new States( CarFactory.createPlayer( carType, carModel ) );
	}

	// 3rd
	public static void createSystems() {
		Systems = new Systems( Environment.b2dWorld, States.playerState.car );
	}

	// 4th
	public static void createWorld( String levelName, boolean nightMode ) {
		Environment.createWorld( Environment.b2dWorld, Environment.scalingStrategy, levelName, nightMode );
	}

	public static void dispose() {
		Director.dispose();
		Art.dispose();

		Environment.dispose();
		Systems.dispose();
	}

	private GameData() {
	}
}
