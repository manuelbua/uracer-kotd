package com.bitfire.uracer.game;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.game.collisions.GameContactListener;
import com.bitfire.uracer.game.effects.TrackEffects;
import com.bitfire.uracer.game.entities.Car;
import com.bitfire.uracer.game.entities.CarEvent;
import com.bitfire.uracer.game.entities.CarFactory;
import com.bitfire.uracer.game.events.DriftStateEvent;
import com.bitfire.uracer.game.events.GameLogicEvent;
import com.bitfire.uracer.game.events.GameRendererEvent;
import com.bitfire.uracer.game.events.PhysicsStepEvent;
import com.bitfire.uracer.game.events.PlayerStateEvent;
import com.bitfire.uracer.game.events.TaskManagerEvent;
import com.bitfire.uracer.game.hud.HudLabel;
import com.bitfire.uracer.game.logic.DriftState;
import com.bitfire.uracer.game.logic.LapState;
import com.bitfire.uracer.game.logic.PhysicsStep;
import com.bitfire.uracer.game.logic.PlayerState;
import com.bitfire.uracer.game.tweening.Tweener;
import com.bitfire.uracer.game.tweening.accessors.BoxedFloatAccessor;
import com.bitfire.uracer.game.tweening.accessors.HudLabelAccessor;
import com.bitfire.uracer.game.tweening.accessors.MessageAccessor;
import com.bitfire.uracer.messager.Message;
import com.bitfire.uracer.messager.Messager;
import com.bitfire.uracer.tiled.ScalingStrategy;
import com.bitfire.uracer.utils.BoxedFloat;

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

	public static Tweener tweener;
	public static World b2dWorld;

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

	public static final class Systems {
		public static PhysicsStep physicsStep;
		public static TrackEffects trackEffects;

		public static void create( World b2dWorld, Car car ) {
			Systems.physicsStep = new PhysicsStep( b2dWorld );
			Systems.trackEffects = new TrackEffects( car );
		}

		public static void dispose() {
			trackEffects.dispose();
		}

		private Systems() {
		}
	}

	public static final class Events {

		public static final GameRendererEvent gameRenderer = new GameRendererEvent();
		public static final PlayerStateEvent playerState = new PlayerStateEvent();
		public static final DriftStateEvent driftState = new DriftStateEvent();
		public static final PhysicsStepEvent physicsStep = new PhysicsStepEvent();
		public static final GameLogicEvent gameLogic = new GameLogicEvent();
		public static final CarEvent carEvent = new CarEvent();
		public static final TaskManagerEvent taskManager = new TaskManagerEvent();

		private Events() {
		}
	}

	public static void create( GameDifficulty difficulty ) {
		// computed for a 256px tile size target (compute needed conversion factors)
		scalingStrategy = new ScalingStrategy( new Vector2( 1280, 800 ), 70f, 224, 1f );

		// everything has been setup on a 256px tile, scale back if that's the case
		Config.Physics.PixelsPerMeter /= (GameData.scalingStrategy.targetScreenRatio / GameData.scalingStrategy.to256);

		GameData.gameSettings = new GameplaySettings( difficulty );
		GameData.tweener = createTweener();
		GameData.b2dWorld = createBox2DWorld();
		GameData.messager = new Messager();
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
		GameData.tweener.clear();
		GameData.tweener.dispose();
		GameData.messager.dispose();
		GameData.States.dispose();
		GameData.Systems.dispose();
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
