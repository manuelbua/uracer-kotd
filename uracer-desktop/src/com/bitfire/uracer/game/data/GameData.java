package com.bitfire.uracer.game.data;

import com.bitfire.uracer.game.GameDifficulty;
import com.bitfire.uracer.game.actors.Car;

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
	}

	// ... (some init, Car created)

	// 2nd
	public static void createStates( Car car ) {
		States = new States( car );
	}

	// 3rd
	public static void createSystems( Car car ) {
		Systems = new Systems( Environment.b2dWorld, car );
	}

	// 4th
	public static void createWorld( String levelName, boolean nightMode ) {
		Environment.createWorld( Environment.b2dWorld, Environment.scalingStrategy, levelName, nightMode );
	}

	public static void dispose() {
		Environment.dispose();
		Systems.dispose();
	}

	private GameData() {
	}
}
