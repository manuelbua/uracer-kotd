package com.bitfire.uracer.game;

import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.data.Environment;
import com.bitfire.uracer.game.data.States;
import com.bitfire.uracer.game.data.Systems;

/** Encapsulates and abstracts the dynamic state of the game.
 *
 * @author bmanuel */
public final class GameData {

	public static States States;
	public static Systems Systems;
	public static Environment Environment;

	public static void create( GameDifficulty difficulty ) {
		Environment = new Environment( difficulty );
	}

	public static void createStates( Car car ) {
		States = new States( car );
	}

	public static void createSystems( World b2dWorld, Car car ) {
		Systems = new Systems( b2dWorld, car );
	}

	public static void dispose() {
		Environment.dispose();
		Systems.dispose();
	}

	public static void createWorld( World b2dWorld, ScalingStrategy strategy, String levelName, boolean nightMode ) {
		Environment.createWorld( b2dWorld, strategy, levelName, nightMode );
	}

	private GameData() {
	}
}
