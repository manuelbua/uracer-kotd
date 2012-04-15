package com.bitfire.uracer.game.data;

import com.bitfire.uracer.Art;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.game.GameDifficulty;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.SpriteBatchUtils;

/** Encapsulates and abstracts the dynamic state of the game.
 *
 * @author bmanuel */
public final class GameData {

	public static Environment Environment;

	// 1st
	public static void create( ScalingStrategy scalingStrategy, GameDifficulty difficulty ) {
		Environment = new Environment( scalingStrategy, difficulty );

		SpriteBatchUtils.init( Art.base6 );
		Convert.init( scalingStrategy.invTileMapZoomFactor, Config.Physics.PixelsPerMeter );
		Director.init();
	}

	public static void dispose() {
		Director.dispose();
		Environment.dispose();
	}

	private GameData() {
	}
}
