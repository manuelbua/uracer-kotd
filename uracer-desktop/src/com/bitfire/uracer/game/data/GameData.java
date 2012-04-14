package com.bitfire.uracer.game.data;

import com.bitfire.uracer.Art;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.game.GameDifficulty;
import com.bitfire.uracer.game.Tweener;
import com.bitfire.uracer.game.player.Car.Aspect;
import com.bitfire.uracer.game.player.CarFactory;
import com.bitfire.uracer.game.player.CarModel;
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
	public static void create( ScalingStrategy scalingStrategy, String levelName, boolean nightMode, GameDifficulty difficulty, Aspect carAspect, CarModel carModel ) {
		Environment = new Environment( scalingStrategy, difficulty );

		Tweener.init();
		BatchUtils.init( Art.base6 );
		Convert.init( GameData.Environment.scalingStrategy.invTileMapZoomFactor, Config.Physics.PixelsPerMeter );
		Director.init();

		States = new States( CarFactory.createPlayer( carAspect, carModel ) );
		Systems = new Systems( Environment.b2dWorld, States.playerState.car );

		// requires Art, Convert and Director
		Environment.createWorld( Environment.b2dWorld, Environment.scalingStrategy, levelName, nightMode );
	}

	public static void dispose() {
		Director.dispose();

		Environment.dispose();
		Systems.dispose();
	}

	private GameData() {
	}
}
