package com.bitfire.uracer.game;

import aurelienribon.tweenengine.Tween;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.CarEvent;
import com.bitfire.uracer.game.actors.CarFactory;
import com.bitfire.uracer.game.collisions.GameContactListener;
import com.bitfire.uracer.game.effects.TrackEffects;
import com.bitfire.uracer.game.events.DriftStateEvent;
import com.bitfire.uracer.game.events.GameLogicEvent;
import com.bitfire.uracer.game.events.GameRendererEvent;
import com.bitfire.uracer.game.events.PhysicsStepEvent;
import com.bitfire.uracer.game.events.PlayerStateEvent;
import com.bitfire.uracer.game.hud.HudLabel;
import com.bitfire.uracer.game.hud.HudLabelAccessor;
import com.bitfire.uracer.game.logic.PhysicsStep;
import com.bitfire.uracer.game.messager.Message;
import com.bitfire.uracer.game.messager.MessageAccessor;
import com.bitfire.uracer.game.messager.Messager;
import com.bitfire.uracer.game.states.DriftState;
import com.bitfire.uracer.game.states.LapState;
import com.bitfire.uracer.game.states.PlayerState;
import com.bitfire.uracer.task.TaskManagerEvent;
import com.bitfire.uracer.utils.BoxedFloat;
import com.bitfire.uracer.utils.BoxedFloatAccessor;

/** Encapsulates and abstracts the dynamic state of the game.
 *
 * @author bmanuel */
public final class GameData {

	private GameData() {
	}

	public static ScalingStrategy scalingStrategy;
	public static GameplaySettings gameSettings;
	public static GameWorld gameWorld;
	public static Messager messager;
	public static World b2dWorld;

	/** States */
	public static final class States {
		public static PlayerState playerState;
		public static DriftState driftState;
		public static LapState lapState;

		public static void create( Car car ) {
			States.playerState = new PlayerState( car, CarFactory.createGhost( car ) );
			States.driftState = new DriftState();
			States.lapState = new LapState();
		}

		public static void dispose() {
			playerState.dispose();
			driftState.dispose();
			States.lapState = null;
		}

		private States() {
		}
	}

	/** Systems */
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

	/** Events */
	public static final class Events {

		public static final GameRendererEvent gameRenderer = new GameRendererEvent();
		public static final PlayerStateEvent playerState = new PlayerStateEvent();
		public static final DriftStateEvent driftState = new DriftStateEvent();
		public static final PhysicsStepEvent physicsStep = new PhysicsStepEvent();
		public static final GameLogicEvent gameLogic = new GameLogicEvent();
		public static final CarEvent carEvent = new CarEvent();

		private Events() {
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

		GameData.messager = new Messager(GameData.scalingStrategy.invTileMapZoomFactor);
	}

	public static void createStates( Car car ) {
		States.create( car );
	}

	public static void createSystems( World b2dWorld, Car car ) {
		Systems.create( b2dWorld, car );
	}

	public static void createWorld( String levelName, boolean nightMode ) {
		gameWorld = new GameWorld( levelName, nightMode );
	}

	public static void dispose() {
		GameData.b2dWorld.dispose();
		GameData.messager.dispose();
		GameData.States.dispose();
		GameData.Systems.dispose();
	}

	private static World createBox2DWorld() {
		World w = new World( new Vector2( 0, 0 ), false );
		w.setContactListener( new GameContactListener() );
		return w;
	}
}
