package com.bitfire.uracer.game;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.factories.CarFactory;
import com.bitfire.uracer.game.collisions.GameContactListener;
import com.bitfire.uracer.game.logic.DriftState;
import com.bitfire.uracer.game.logic.LapState;
import com.bitfire.uracer.game.logic.PlayerState;
import com.bitfire.uracer.hud.Hud;
import com.bitfire.uracer.hud.HudLabel;
import com.bitfire.uracer.messager.Message;
import com.bitfire.uracer.tiled.ScalingStrategy;
import com.bitfire.uracer.tweener.BoxedFloat;
import com.bitfire.uracer.tweener.Tweener;
import com.bitfire.uracer.tweener.accessors.BoxedFloatAccessor;
import com.bitfire.uracer.tweener.accessors.HudLabelAccessor;
import com.bitfire.uracer.tweener.accessors.MessageAccessor;

/** Encapsulates and abstracts the dynamic state of the game.
 *
 * @author bmanuel */
public class GameData {

	public static ScalingStrategy scalingStrategy;
	public static GameplaySettings gameSettings;
	public static GameWorld gameWorld;

	public static Tweener tweener;
	public static World b2dWorld;
	public static Hud hud;

	// state
	public static PlayerState playerState;
	public static DriftState driftState;
	public static LapState lapState;

	public static void create( GameDifficulty difficulty ) {
		// computed for a 256px tile size target (need conversion)
		scalingStrategy = new ScalingStrategy( new Vector2( 1280, 800 ), 70f, 224, 1f );

		// everything has been setup on a 256px tile, scale back if that's the case
		Config.Physics.PixelsPerMeter /= (GameData.scalingStrategy.targetScreenRatio / GameData.scalingStrategy.to256);

		GameData.tweener = createTweener();
		GameData.b2dWorld = createBox2DWorld();

		GameData.gameSettings = new GameplaySettings( difficulty );
	}

	public static void createStates(Car car) {
		GameData.playerState = new PlayerState(car, CarFactory.createGhost( car ));
		GameData.driftState = new DriftState();
		GameData.lapState = new LapState();
	}

	public static void dispose() {
		GameData.b2dWorld.dispose();
		GameData.hud.dispose();
		GameData.tweener.clear();
		GameData.tweener.dispose();
	}

	private static Tweener createTweener() {
		Tweener t = new Tweener();
		Tweener.registerAccessor( Message.class, new MessageAccessor() );
		Tweener.registerAccessor( HudLabel.class, new HudLabelAccessor() );
		Tweener.registerAccessor( BoxedFloat.class, new BoxedFloatAccessor() );
		return t;
	}

	private static World createBox2DWorld() {
		World w = new World( new Vector2( 0, 0 ), false );
		w.setContactListener( new GameContactListener() );
		return w;
	}
}
