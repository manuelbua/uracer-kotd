package com.bitfire.uracer.game;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.effects.TrackEffects;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.events.CarEvent;
import com.bitfire.uracer.events.DriftStateEvent;
import com.bitfire.uracer.events.GameLogicEvent;
import com.bitfire.uracer.events.GameRendererEvent;
import com.bitfire.uracer.events.PhysicsStepEvent;
import com.bitfire.uracer.events.PlayerStateEvent;
import com.bitfire.uracer.events.TaskManagerEvent;
import com.bitfire.uracer.factories.CarFactory;
import com.bitfire.uracer.game.collisions.GameContactListener;
import com.bitfire.uracer.game.logic.DriftState;
import com.bitfire.uracer.game.logic.LapState;
import com.bitfire.uracer.game.logic.PhysicsStep;
import com.bitfire.uracer.game.logic.PlayerState;
import com.bitfire.uracer.hud.HudLabel;
import com.bitfire.uracer.messager.Message;
import com.bitfire.uracer.messager.Messager;
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

	private GameData() {
	}

	public static ScalingStrategy scalingStrategy;
	public static GameplaySettings gameSettings;
	public static GameWorld gameWorld;
	public static Messager messager;

	public static Tweener tweener;
	public static World b2dWorld;

	public static final class State {
		public static PlayerState playerState;
		public static DriftState driftState;
		public static LapState lapState;

		public static void dispose() {
			playerState.dispose();
			driftState.dispose();
		}
	}

	public static final class System {
		public static PhysicsStep physicsStep;
		public static TrackEffects trackEffects;

		public static void dispose() {
			trackEffects.dispose();
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
		State.playerState = new PlayerState( car, CarFactory.createGhost( car ) );
		State.driftState = new DriftState();
		State.lapState = new LapState();
	}

	public static void createSystems() {
		System.physicsStep = new PhysicsStep( GameData.b2dWorld );
		System.trackEffects = new TrackEffects( State.playerState.car );
	}

	public static void createWorld( String levelName, boolean nightMode ) {
		gameWorld = new GameWorld( levelName, nightMode );
	}

	public static void dispose() {
		GameData.b2dWorld.dispose();
		GameData.tweener.clear();
		GameData.tweener.dispose();
		GameData.messager.dispose();
		GameData.State.dispose();
		GameData.System.dispose();
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
