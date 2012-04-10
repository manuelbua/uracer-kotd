package com.bitfire.uracer.game;

import aurelienribon.tweenengine.Tween;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.collisions.GameContactListener;
import com.bitfire.uracer.game.data.States;
import com.bitfire.uracer.game.effects.TrackEffects;
import com.bitfire.uracer.game.hud.HudLabel;
import com.bitfire.uracer.game.hud.HudLabelAccessor;
import com.bitfire.uracer.game.logic.PhysicsStep;
import com.bitfire.uracer.game.messager.Message;
import com.bitfire.uracer.game.messager.MessageAccessor;
import com.bitfire.uracer.game.messager.Messager;
import com.bitfire.uracer.task.TaskManagerEvent;
import com.bitfire.uracer.utils.BoxedFloat;
import com.bitfire.uracer.utils.BoxedFloatAccessor;

/** Encapsulates and abstracts the dynamic state of the game.
 *
 * @author bmanuel */
public final class GameData {

	private GameData() {
	}

	public static States States;

	public static ScalingStrategy scalingStrategy;
	public static GameplaySettings gameSettings;
	public static GameWorld gameWorld;
	public static Messager messager;
	public static World b2dWorld;

	// @formatter:off
	/** Systems
	 *
	 * This is the order in which tickable and Tasks are being dispatched and consumed by the game super components:
	 *
	 * 1	Input task
	 * 2	PhysicsStep task
	 * 3	any other task
	 * 4	Time timers
	 * 5	GameLogic updates playState and driftState
	 *
	 **/
	// @formatter:on
	public static final class Systems {
		public static PhysicsStep physicsStep;
		public static TrackEffects trackEffects;

		public static void create( World b2dWorld, Car car ) {
			Systems.input = new Input( TaskManagerEvent.Order.MINUS_4 );
			Systems.physicsStep = new PhysicsStep( b2dWorld, TaskManagerEvent.Order.MINUS_3 );

			// FIXME this is just for GameRenderer, Hud and GameData
			Systems.trackEffects = new TrackEffects( car );
		}

		public static void dispose() {
			trackEffects.dispose();
		}

		public static Input input;

		private Systems() {
		}
	}

	public static void create( GameDifficulty difficulty ) {
		// computed for a 256px tile size target (compute needed conversion factors)
		scalingStrategy = new ScalingStrategy( new Vector2( 1280, 800 ), 70f, 224, 1f );

		// everything has been setup on a 256px tile, scale back if that's the case
		Config.Physics.PixelsPerMeter /= (GameData.scalingStrategy.targetScreenRatio / GameData.scalingStrategy.to256);

		GameData.gameSettings = new GameplaySettings( difficulty );
		GameData.b2dWorld = createBox2DWorld();

		Tween.registerAccessor( Message.class, new MessageAccessor() );
		Tween.registerAccessor( HudLabel.class, new HudLabelAccessor() );
		Tween.registerAccessor( BoxedFloat.class, new BoxedFloatAccessor() );

		GameData.messager = new Messager( GameData.scalingStrategy.invTileMapZoomFactor );
	}

	public static void createStates( Car car ) {
		States = new States( car );
	}

	public static void createSystems( World b2dWorld, Car car ) {
		Systems.create( b2dWorld, car );
	}

	public static void createWorld( World b2dWorld, ScalingStrategy strategy, String levelName, boolean nightMode ) {
		gameWorld = new GameWorld( b2dWorld, strategy, levelName, nightMode );
	}

	public static void dispose() {
		GameData.b2dWorld.dispose();
		GameData.messager.dispose();
		GameData.Systems.dispose();
	}

	private static World createBox2DWorld() {
		World w = new World( new Vector2( 0, 0 ), false );
		w.setContactListener( new GameContactListener() );
		return w;
	}
}
