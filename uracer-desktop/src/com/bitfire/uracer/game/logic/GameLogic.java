package com.bitfire.uracer.game.logic;

import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.TweenEquation;
import aurelienribon.tweenengine.equations.Sine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.g2d.tiled.TiledLayer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.game.GameplaySettings;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.Car.Aspect;
import com.bitfire.uracer.game.actors.CarEvent;
import com.bitfire.uracer.game.actors.CarFactory;
import com.bitfire.uracer.game.actors.CarModel;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.collisions.GameContactListener;
import com.bitfire.uracer.game.events.PlayerCarStateEvent;
import com.bitfire.uracer.game.events.PlayerDriftStateEvent;
import com.bitfire.uracer.game.input.Input;
import com.bitfire.uracer.game.input.Replay;
import com.bitfire.uracer.game.logic.helpers.DirectorController;
import com.bitfire.uracer.game.logic.helpers.Recorder;
import com.bitfire.uracer.game.logic.hud.Hud;
import com.bitfire.uracer.game.logic.hud.HudLabel;
import com.bitfire.uracer.game.logic.hud.HudLabelAccessor;
import com.bitfire.uracer.game.logic.hud.elements.HudDrifting;
import com.bitfire.uracer.game.logic.hud.elements.HudDrifting.EndDriftType;
import com.bitfire.uracer.game.logic.notifier.Message;
import com.bitfire.uracer.game.logic.notifier.Message.MessagePosition;
import com.bitfire.uracer.game.logic.notifier.Message.MessageSize;
import com.bitfire.uracer.game.logic.notifier.Message.Type;
import com.bitfire.uracer.game.logic.notifier.MessageAccessor;
import com.bitfire.uracer.game.logic.notifier.Notifier;
import com.bitfire.uracer.game.logic.sounds.SoundEffect;
import com.bitfire.uracer.game.logic.sounds.SoundManager;
import com.bitfire.uracer.game.logic.sounds.effects.PlayerDriftSoundEffect;
import com.bitfire.uracer.game.logic.sounds.effects.PlayerImpactSoundEffect;
import com.bitfire.uracer.game.logic.trackeffects.TrackEffects;
import com.bitfire.uracer.game.logic.trackeffects.effects.PlayerSkidMarks;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.states.LapState;
import com.bitfire.uracer.game.tween.GameTweener;
import com.bitfire.uracer.game.tween.WcTweener;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.game.world.WorldDefs.TileLayer;
import com.bitfire.uracer.task.TaskManagerEvent;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.BoxedFloat;
import com.bitfire.uracer.utils.BoxedFloatAccessor;
import com.bitfire.uracer.utils.NumberString;

/** This concrete class manages to handle the inner game logic and its evolving states: it's all about gamedata, baby!
 * This should be refactored into smaller pieces of logic, that's where the components should came into.
 *
 * @author bmanuel */
public class GameLogic implements CarEvent.Listener, PlayerCarStateEvent.Listener, PlayerDriftStateEvent.Listener {
	// event
//	public final GameLogicEvent event = new GameLogicEvent();

	// scaling
	private ScalingStrategy scalingStrategy = null;

	// settings
	private GameplaySettings gameplaySettings = null;

	// world
	private GameWorld gameWorld = null;
	private World box2dWorld = null;

	// input system
	private Input input = null;

	// physics step
	private PhysicsStep physicsStep;

	// player
	private PlayerCar playerCar = null;
	private GhostCar playerGhostCar = null;

	// lap
	private boolean isFirstLap = true;
	private long lastRecordedLapId = 0;

	private DirectorController controller = null;

	// replay
	private Recorder recorder = null;

	// tasks
	private GameTasksManager gameTasksManager = null;

	// special effects
	private TrackEffects effects = null;

	// hud
	private Hud hud = null;
	private HudDrifting hudDrifting = null;

	// sound
	private SoundManager sound = null;

	// alerts and infos
	private Notifier messager = null;

	// states
	private LapState playerLapState = null;

	// handles timeModulationBusy onComplete event
	boolean timeModulation = false, timeModulationBusy = false;
	BoxedFloat timeMultiplier = new BoxedFloat();
	float tmMin = 0.3f;
	TweenCallback timeModulationFinished = new TweenCallback() {
		@Override
		public void onEvent( int type, BaseTween<?> source ) {
			switch( type ) {
			case COMPLETE:
				timeModulationBusy = false;
			}
		}
	};

	public GameLogic( GameplaySettings settings, ScalingStrategy scalingStrategy, String levelName, Aspect carAspect, CarModel carModel ) {
		this.gameplaySettings = settings;
		this.scalingStrategy = scalingStrategy;
		this.box2dWorld = new World( new Vector2( 0, 0 ), false );
		this.box2dWorld.setContactListener( new GameContactListener() );

		// initializes the Director helper
		Director.init();

		// create tweening support
		createTweeners();
		Gdx.app.log( "GameLogic", "Helpers created" );

		gameWorld = new GameWorld( box2dWorld, scalingStrategy, levelName, false );
		Gdx.app.log( "GameLogic", "Game world ready" );

		recorder = new Recorder();
		timeMultiplier.value = 1f;

		playerLapState = new LapState();

		// creates global camera controller
		controller = new DirectorController( Config.Graphics.CameraInterpolationMode, Director.halfViewport, gameWorld.worldSizeScaledPx, gameWorld.worldSizeTiles );

		// creates player and ghost cars
		createPlayer( gameWorld, carAspect, carModel );
		Gdx.app.log( "GameLogic", "Player created" );

		createGameTasks( playerLapState );
		configureTasks( playerCar );
		Gdx.app.log( "GameLogic", "Game tasks created and configured" );

		configurePlayer( gameplaySettings, gameWorld, playerCar, input );
		Gdx.app.log( "GameLogic", "Player configured" );

		// subscribe to player-related events
		registerPlayerEvents( playerCar );
		Gdx.app.log( "GameLogic", "Registered player-related events" );

		// messager.show( "COOL STUFF!", 60, Message.Type.Information, MessagePosition.Bottom, MessageSize.Big );
	}

	public void dispose() {
		gameTasksManager.dispose();

		if( playerCar != null ) {
			playerCar.dispose();
		}

		if( playerGhostCar != null ) {
			playerGhostCar.dispose();
		}

		gameWorld.dispose();
		GameTweener.dispose();
		WcTweener.dispose();
		Director.dispose();
		box2dWorld.dispose();
	}

	public GameWorld getGameWorld() {
		return gameWorld;
	}

	public PlayerCar getPlayer() {
		return playerCar;
	}

	public World getBox2dWorld() {
		return box2dWorld;
	}

	private void registerPlayerEvents( PlayerCar player ) {
		player.carState.event.addListener( this, PlayerCarStateEvent.Type.onTileChanged );
		player.driftState.event.addListener( this, PlayerDriftStateEvent.Type.onBeginDrift );
		player.driftState.event.addListener( this, PlayerDriftStateEvent.Type.onEndDrift );
		player.event.addListener( this, CarEvent.Type.onCollision );
		player.event.addListener( this, CarEvent.Type.onComputeForces );
	}

	private void createTweeners() {
		Tween.registerAccessor( Message.class, new MessageAccessor() );
		Tween.registerAccessor( HudLabel.class, new HudLabelAccessor() );
		Tween.registerAccessor( BoxedFloat.class, new BoxedFloatAccessor() );
	}

	private void createGameTasks( LapState lapState ) {
		gameTasksManager = new GameTasksManager();

		// input system
		input = new Input( TaskManagerEvent.Order.MINUS_4 );

		// physics step
		physicsStep = new PhysicsStep( box2dWorld, TaskManagerEvent.Order.MINUS_3 );

		// sound manager
		sound = new SoundManager();
		gameTasksManager.add( sound );

		// message manager
		messager = new Notifier( scalingStrategy.invTileMapZoomFactor );
		gameTasksManager.add( messager );

		// hud manager
		hud = new Hud( scalingStrategy, lapState );
		gameTasksManager.add( hud );

		// effects manager
		effects = new TrackEffects();
		gameTasksManager.add( effects );
	}

	private void configureTasks( PlayerCar player ) {
		// sounds
		SoundEffect fx = new PlayerDriftSoundEffect( player );
		fx.start();
		sound.add( fx );
		sound.add( new PlayerImpactSoundEffect( player ) );

		// track effects
		PlayerSkidMarks carSkidMarks = new PlayerSkidMarks( player );
		effects.add( carSkidMarks );

		// hud
		hudDrifting = new HudDrifting( scalingStrategy, player );
		hud.add( hudDrifting );
	}

	private void createPlayer( GameWorld gameWorld, Aspect carAspect, CarModel carModel ) {
		playerCar = CarFactory.createPlayer( box2dWorld, gameWorld, input, carAspect, carModel );
		playerGhostCar = CarFactory.createGhost( box2dWorld, gameWorld, playerCar );
	}

	private void configurePlayer( GameplaySettings settings, GameWorld world, PlayerCar player, Input input ) {
		// create player and setup player input system and initial position in the world
		player.setTransform( world.playerStartPos, world.playerStartOrient );
		player.setInputSystem( input );

		// apply handicaps
		player.setLinearVelocityDampingAF( settings.linearVelocityDampingAfterFeedback );
		player.setThrottleDampingAF( settings.throttleDampingAfterFeedback );
	}

	public boolean onTick() {
		if( input.isOn( Keys.R ) ) {
			restartLogic();
			gameTasksManager.restart();
//			event.trigger( GameLogicEvent.Type.onRestart );
		} else if( input.isOn( Keys.T ) ) {
			restartLogic();
			resetLogic();
			gameTasksManager.reset();
//			event.trigger( GameLogicEvent.Type.onReset );
		} else if( input.isOn( Keys.Q ) ) {
			Gdx.app.exit();
			return false;
		} else if( input.isOn( Keys.SPACE ) && !timeModulationBusy ) {

			TweenEquation eqIn = Sine.INOUT;
			TweenEquation eqOut = Sine.INOUT;

			timeModulation = !timeModulation;
			if( timeModulation ) {
				timeModulationBusy = true;
				WcTweener.start( Timeline.createSequence().push( Tween.to( timeMultiplier, BoxedFloatAccessor.VALUE, 1000 ).target( tmMin ).ease( eqIn ) )
						.setCallback( timeModulationFinished ) );
			} else {
				timeModulationBusy = true;
				WcTweener.start( Timeline.createSequence()
						.push( Tween.to( timeMultiplier, BoxedFloatAccessor.VALUE, 1000 ).target( Config.Physics.PhysicsTimeMultiplier ).ease( eqOut ) )
						.setCallback( timeModulationFinished ) );
			}
		}

		updatePlayerCarFriction();
		updateTimeMultiplier();

		return true;
	}

	private void updateTimeMultiplier() {
		URacer.timeMultiplier = AMath.clamp( timeMultiplier.value, tmMin, Config.Physics.PhysicsTimeMultiplier );
	}

	//
	// RENDERING-BOUND LOGIC
	//

	public void onBeforeRender() {
		// trigger the event and let's subscribers interpolate and update their state()
		physicsStep.triggerOnTemporalAliasing( URacer.getTemporalAliasing() );

		if( playerCar != null ) {
			Vector2 carpos = playerCar.state().position;

			// camera follows the player's car
			controller.setPosition( carpos );
		}

		// tweener step
		WcTweener.update();
		GameTweener.update();
	}

	//
	// TODO, COULD THIS BE A TASK HANDLING IN-GAME USER CHOICES ??
	//

	private void resetPlayer( Car playerCar, GhostCar playerGhostCar ) {
		if( playerCar != null ) {
			playerCar.reset();
			playerCar.setTransform( gameWorld.playerStartPos, gameWorld.playerStartOrient );
		}

		if( playerGhostCar != null ) {
			playerGhostCar.reset();
		}
	}

	private void restartLogic() {
		resetPlayer( playerCar, playerGhostCar );
		isFirstLap = true;
		timeModulationBusy = false;
		timeModulation = false;
		timeMultiplier.value = Config.Physics.PhysicsTimeMultiplier;
		WcTweener.clear();
		GameTweener.clear();
		recorder.reset();
		gameTasksManager.restart();
	}

	private void resetLogic() {
		lastRecordedLapId = 0;
		playerLapState.reset();
		gameTasksManager.reset();
	}

	//
	// EVENT HANDLERS
	//

	@Override
	public void carEvent( CarEvent.Type type, CarEvent.Data data ) {
		switch( type ) {
		case onCollision:
			if( playerCar.driftState.isDrifting ) {
				playerCar.driftState.invalidateByCollision();
			}
			break;
		case onComputeForces:
			if( recorder.isRecording() ) {
				recorder.add( data.forces );
			}
			break;
		}
	};

	@Override
	public void playerCarStateEvent( Car source, PlayerCarStateEvent.Type type ) {
		switch( type ) {
		case onTileChanged:
			updatePlayerLapState( playerLapState );
			break;
		}
	}

	// Pay attention here! Multiple DriftState objects trigger the same event! Check the source for
	// handling multiple and crossing beginDrift/endDrift calls!
	//
	// Anyway, we can't track GhostCar's drift states since we record the forces generated by the CarSimulator!
	@Override
	public void playerDriftStateEvent( PlayerCar player, PlayerDriftStateEvent.Type type ) {
		switch( type ) {
		case onBeginDrift:
			hudDrifting.beginDrift();
			break;
		case onEndDrift:
			String seconds = NumberString.format( player.driftState.driftSeconds() ) + "  seconds!";
			boolean driftEndByCollision = player.driftState.hasCollided;
			float driftSeconds = player.driftState.driftSeconds();

			if( !driftEndByCollision ) {
				if( driftSeconds >= 1 && driftSeconds < 3f ) {
					messager.enqueue( "NICE ONE!\n+" + seconds, 1f, Type.Good, MessagePosition.Middle, MessageSize.Big );
				} else if( driftSeconds >= 3f && driftSeconds < 5f ) {
					messager.enqueue( "FANTASTIC!\n+" + seconds, 1f, Type.Good, MessagePosition.Middle, MessageSize.Big );
				} else if( driftSeconds >= 5f ) {
					messager.enqueue( "UNREAL!\n+" + seconds, 1f, Type.Good, MessagePosition.Bottom, MessageSize.Big );
				}

				hudDrifting.endDrift( "+" + NumberString.format( driftSeconds ), EndDriftType.GoodDrift );

			} else {
				hudDrifting.endDrift( "-" + NumberString.format( driftSeconds ), EndDriftType.BadDrift );
			}

			break;
		}
	}

	//
	// MORE TASKS ??
	//

	private Vector2 offset = new Vector2();

	private void updatePlayerCarFriction() {
		if( playerCar != null ) {
			Vector2 tilePosition = playerCar.carState.tilePosition;

			if( gameWorld.isValidTilePosition( tilePosition ) ) {
				// compute realsize-based pixel offset car-tile (top-left origin)
				float scaledTileSize = gameWorld.getTileSizeScaled();
				float tsx = tilePosition.x * scaledTileSize;
				float tsy = tilePosition.y * scaledTileSize;
				offset.set( playerCar.state().position );
				offset.y = gameWorld.worldSizeScaledPx.y - offset.y;
				offset.x = offset.x - tsx;
				offset.y = offset.y - tsy;
				offset.mul( gameWorld.getTileSizeInvScaled() ).mul( gameWorld.map.tileWidth );

				TiledLayer layerTrack = gameWorld.getLayer( TileLayer.Track );
				int id = layerTrack.tiles[(int)tilePosition.y][(int)tilePosition.x] - 1;

				// int xOnMap = (id %4) * 224 + (int)offset.x;
				// int yOnMap = (int)( id/4f ) * 224 + (int)offset.y;

				// bit twiddling, faster version
				int xOnMap = (id & 3) * (int)gameWorld.map.tileWidth + (int)offset.x;
				int yOnMap = (id >> 2) * (int)gameWorld.map.tileWidth + (int)offset.y;

				int pixel = Art.frictionNature.getPixel( xOnMap, yOnMap );
				playerCar.setFriction( (pixel == -256 ? 0 : -1) );
			} else {
				Gdx.app.log( "GameLogic", "PlayerCar out of map!" );
			}
		}
	}

	// FIXME looks like this function is doing MUCH more than what's stated in its name..
	private void updatePlayerLapState( LapState lapState ) {
		if( playerCar != null ) {
			boolean onStartZone = (playerCar.carState.currTileX == gameWorld.playerStartTileX && playerCar.carState.currTileY == gameWorld.playerStartTileY);

			String name = gameWorld.name;

			if( onStartZone ) {
				if( isFirstLap ) {
					isFirstLap = false;

					lapState.restart();
					Replay buf = lapState.getNextBuffer();
					recorder.beginRecording( playerCar, buf, name, gameplaySettings.difficulty );
					lastRecordedLapId = buf.id;

					if( lapState.hasAnyReplayData() ) {
						Replay any = lapState.getAnyReplay();
						playerGhostCar.setReplay( any );
					}
				} else {
					if( recorder.isRecording() ) {
						recorder.endRecording();
					}

					lapState.updateReplays();

					// replay best, overwrite worst logic

					if( !lapState.hasAllReplayData() ) {
						// only one single replay
						lapState.restart();
						Replay buf = lapState.getNextBuffer();
						recorder.beginRecording( playerCar, buf, name, gameplaySettings.difficulty );
						lastRecordedLapId = buf.id;

						Replay any = lapState.getAnyReplay();
						playerGhostCar.setReplay( any );
						lapState.setLastTrackTimeSeconds( any.trackTimeSeconds );

						messager.show( "GO!  GO!  GO!", 3f, Type.Information, MessagePosition.Middle, MessageSize.Big );
					} else {
						// both valid, replay best, overwrite worst
						Replay best = lapState.getBestReplay(), worst = lapState.getWorstReplay();

						if( lastRecordedLapId == best.id ) {
							lapState.setLastTrackTimeSeconds( best.trackTimeSeconds );
							messager.show( "-" + NumberString.format( worst.trackTimeSeconds - best.trackTimeSeconds ) + " seconds!", 3f, Type.Good, MessagePosition.Top,
									MessageSize.Big );
						} else {
							lapState.setLastTrackTimeSeconds( worst.trackTimeSeconds );
							messager.show( "+" + NumberString.format( worst.trackTimeSeconds - best.trackTimeSeconds ) + " seconds", 3f, Type.Bad, MessagePosition.Top,
									MessageSize.Big );
						}

						playerGhostCar.setReplay( best );

						lapState.restart();
						recorder.beginRecording( playerCar, worst, name, gameplaySettings.difficulty );
						lastRecordedLapId = worst.id;
					}
				}
			}
		}
	}
}
