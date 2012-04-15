package com.bitfire.uracer.game.data;

import com.bitfire.uracer.Config;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.game.GameDifficulty;
import com.bitfire.uracer.game.GameplaySettings;

public final class Environment {

	public GameplaySettings gameSettings;

	public Environment( ScalingStrategy scalingStrategy, GameDifficulty difficulty ) {
		// everything has been setup on a 256px tile, scale back if that's the case
		Config.Physics.PixelsPerMeter /= (scalingStrategy.targetScreenRatio / scalingStrategy.to256);

		gameSettings = new GameplaySettings( difficulty );
	}
}