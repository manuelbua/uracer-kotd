package com.bitfire.uracer.game.logic;

import java.util.ArrayList;
import java.util.List;

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
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.GameplaySettings;
import com.bitfire.uracer.game.Input;
import com.bitfire.uracer.game.Replay;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.Car.Aspect;
import com.bitfire.uracer.game.actors.Car.InputMode;
import com.bitfire.uracer.game.actors.CarEvent;
import com.bitfire.uracer.game.actors.CarFactory;
import com.bitfire.uracer.game.actors.CarModel;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.audio.CarDriftSoundEffect;
import com.bitfire.uracer.game.audio.CarImpactSoundEffect;
import com.bitfire.uracer.game.collisions.GameContactListener;
import com.bitfire.uracer.game.events.DriftStateEvent;
import com.bitfire.uracer.game.events.GameLogicEvent;
import com.bitfire.uracer.game.events.PlayerStateEvent;
import com.bitfire.uracer.game.logic.helpers.DirectorController;
import com.bitfire.uracer.game.logic.helpers.Recorder;
import com.bitfire.uracer.game.logic.hud.Hud;
import com.bitfire.uracer.game.logic.hud.HudDrifting;
import com.bitfire.uracer.game.logic.hud.HudDrifting.EndDriftType;
import com.bitfire.uracer.game.logic.hud.HudLabel;
import com.bitfire.uracer.game.logic.hud.HudLabelAccessor;
import com.bitfire.uracer.game.logic.sounds.ISoundEffect;
import com.bitfire.uracer.game.logic.sounds.SoundManager;
import com.bitfire.uracer.game.logic.trackeffects.CarSkidMarks;
import com.bitfire.uracer.game.logic.trackeffects.TrackEffects;
import com.bitfire.uracer.game.messager.Message;
import com.bitfire.uracer.game.messager.Message.MessagePosition;
import com.bitfire.uracer.game.messager.Message.MessageSize;
import com.bitfire.uracer.game.messager.Message.Type;
import com.bitfire.uracer.game.messager.MessageAccessor;
import com.bitfire.uracer.game.messager.Messager;
import com.bitfire.uracer.game.states.DriftState;
import com.bitfire.uracer.game.states.LapState;
import com.bitfire.uracer.game.states.PlayerState;
import com.bitfire.uracer.game.tween.GameTweener;
import com.bitfire.uracer.game.tween.WcTweener;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.game.world.WorldDefs.TileLayer;
import com.bitfire.uracer.task.Task;
import com.bitfire.uracer.task.TaskManagerEvent;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.BoxedFloat;
import com.bitfire.uracer.utils.BoxedFloatAccessor;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.NumberString;

// TODO, GameTasks entity for managing them with get(name)/get(id)? Opening up to Components interacting with each
// other? I don't quite like that..
public class GameLogic implements CarEvent.Listener, PlayerStateEvent.Listener, DriftStateEvent.Listener {
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
	private Car playerCar = null;
	private GhostCar playerGhostCar = null;

	// lap
	private boolean isFirstLap = true;
	private long lastRecordedLapId = 0;

	private DirectorController controller = null;

	// replay
	private Recorder recorder = null;

	// tasks
	private List<Task> gameTasks = null;

	// special effects
	private TrackEffects effects = null;
	private CarSkidMarks playerSkidMarks = null;

	// hud
	private Hud hud = null;
	private HudDrifting hudDrifting = null;

	// sound
	private SoundManager sound = null;

	// alerts and infos
	private Messager messager = null;

	// states
	private PlayerState playerState = null;
	private DriftState playerDriftState = null;
	private LapState lapState = null;

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

		// register event handlers
		GameEvents.playerState.addListener( this );
		GameEvents.carEvent.addListener( this, CarEvent.Type.onCollision );
		GameEvents.carEvent.addListener( this, CarEvent.Type.onComputeForces );
		GameEvents.playerDriftState.addListener( this, DriftStateEvent.Type.onBeginDrift );
		GameEvents.playerDriftState.addListener( this, DriftStateEvent.Type.onEndDrift );

		// initializes the Director helper
		Director.init();

		// create tweening support
		Tween.registerAccessor( Message.class, new MessageAccessor() );
		Tween.registerAccessor( HudLabel.class, new HudLabelAccessor() );
		Tween.registerAccessor( BoxedFloat.class, new BoxedFloatAccessor() );

		// wall-clocked tweener, use this to tween the timeMultiplier
		WcTweener.init();

		// game tweener, for all the rest
		GameTweener.init();

		gameWorld = new GameWorld( box2dWorld, scalingStrategy, levelName, false );

		recorder = new Recorder();
		timeMultiplier.value = 1f;

		playerCar = CarFactory.createPlayer( box2dWorld, input, carAspect, carModel );
		playerGhostCar = CarFactory.createGhost( box2dWorld, playerCar );

		createStates( playerCar, playerGhostCar );

		// creates global camera controller
		controller = new DirectorController( Config.Graphics.CameraInterpolationMode, Director.halfViewport, gameWorld.worldSizeScaledPx, gameWorld.worldSizeTiles );

		createGameTasks();
		configureTasks();

		configurePlayer( gameplaySettings, gameWorld, playerCar );

		// messager.show( "COOL STUFF!", 60, Message.Type.Information, MessagePosition.Bottom, MessageSize.Big );
	}

	public void dispose() {
		for( Task task : gameTasks ) {
			task.dispose();
		}

		gameTasks.clear();

		if( playerState.car != null ) {
			playerState.car.dispose();
		}

		if( playerState.ghost != null ) {
			playerState.ghost.dispose();
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

	public Car getPlayer() {
		return playerCar;
	}

	public World getBox2dWorld() {
		return box2dWorld;
	}

	private void createStates( Car player, GhostCar playerGhost ) {
		playerState = new PlayerState( gameWorld, player, playerGhost );
		playerDriftState = new DriftState( player );
		lapState = new LapState();
	}

	private void createGameTasks() {
		gameTasks = new ArrayList<Task>( 10 );

		// input system
		input = new Input( TaskManagerEvent.Order.MINUS_4 );

		// physics step
		physicsStep = new PhysicsStep( box2dWorld, TaskManagerEvent.Order.MINUS_3 );

		// sound manager
		sound = new SoundManager();
		gameTasks.add( sound );

		// message manager
		messager = new Messager( scalingStrategy.invTileMapZoomFactor );
		gameTasks.add( messager );

		// hud manager
		hud = new Hud( scalingStrategy );
		gameTasks.add( hud );

		// effects manager
		effects = new TrackEffects();
		gameTasks.add( effects );
	}

	private void configureTasks() {
		CarModel model = playerCar.getCarModel();
		float carModelWithPx = Convert.mt2px( model.width );
		float carModelLengthPx = Convert.mt2px( model.length );

		// sounds
		ISoundEffect fx = new CarDriftSoundEffect( playerState, playerDriftState );
		fx.start();
		sound.add( fx );
		sound.add( new CarImpactSoundEffect( playerState ) );

		// track effects
		playerSkidMarks = new CarSkidMarks( carModelWithPx, carModelLengthPx );
		effects.add( playerSkidMarks );

		// hud
		hudDrifting = new HudDrifting( scalingStrategy, carModelWithPx, carModelLengthPx );
		hud.add( hudDrifting );
	}

	private void configurePlayer( GameplaySettings settings, GameWorld world, Car player ) {
		// create player and setup player input system and initial position in the world
		player.setTransform( world.playerStartPos, world.playerStartOrient );
		player.setInputSystem( input );

		// apply handicaps
		player.setLinearVelocityDampingAF( settings.linearVelocityDampingAfterFeedback );
		player.setThrottleDampingAF( settings.throttleDampingAfterFeedback );
	}

	public boolean onTick() {
		if( input.isOn( Keys.R ) ) {
			restart();
			GameEvents.gameLogic.trigger( GameLogicEvent.Type.onRestart );
		} else if( input.isOn( Keys.T ) ) {
			restart();
			reset();
			GameEvents.gameLogic.trigger( GameLogicEvent.Type.onReset );
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

		updateStates();
		updateCarFriction();
		updateGameTasks();
		updateTimeMultiplier();

		return true;
	}

	private void updateGameTasks() {
		updateHud();
		updateTrackEffects();
	}

	private void updateTrackEffects() {
		// update track effects
		if( playerCar.getCarDescriptor().velocity_wc.len2() >= 1 ) {
			playerSkidMarks.tryAddDriftMark( playerCar.state().position, playerCar.state().orientation, playerDriftState );
		}
	}

	private void updateStates() {
		playerState.update();

		// compute drift state for player's car
		playerDriftState.update();
	}

	private void updateTimeMultiplier() {
		URacer.timeMultiplier = AMath.clamp( timeMultiplier.value, tmMin, Config.Physics.PhysicsTimeMultiplier );
	}

	private void updateHud() {
		hud.update( lapState );
		hudDrifting.update( playerDriftState );
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

			// hud keeps track of the player's position and orientation
			hud.trackPlayerPosition( playerCar );
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

	private void restart() {
		resetPlayer( playerCar, playerGhostCar );
		isFirstLap = true;
		timeModulationBusy = false;
		timeModulation = false;
		timeMultiplier.value = Config.Physics.PhysicsTimeMultiplier;
	}

	private void reset() {
		restart();
		lastRecordedLapId = 0;
	}

	//
	// EVENT HANDLERS
	//

	@Override
	public void carEvent( CarEvent.Type type, CarEvent.Data data ) {
		switch( type ) {
		case onCollision:
			Car car = GameEvents.carEvent.data.car;
			boolean isPlayerCar = (car.getInputMode() == InputMode.InputFromPlayer);
			if( isPlayerCar && playerDriftState.isDrifting ) {
				playerDriftState.invalidateByCollision();
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
	public void playerStateEvent( PlayerStateEvent.Type type ) {
		switch( type ) {
		case onTileChanged:
			updateLap();
			break;
		}
	}

	@Override
	public void driftStateEvent( DriftStateEvent.Type type ) {
		switch( type ) {
		case onBeginDrift:
			// prologue, fades in and starts showing
			// accumulated drifting time in realtime
			hudDrifting.beginDrift();
			break;
		case onEndDrift:
			String seconds = NumberString.format( playerDriftState.driftSeconds() ) + "  seconds!";

			float driftSeconds = playerDriftState.driftSeconds();
			if( driftSeconds >= 1 && driftSeconds < 3f ) {
				messager.enqueue( "NICE ONE!\n+" + seconds, 1f, Type.Good, MessagePosition.Middle, MessageSize.Big );
			} else if( driftSeconds >= 3f && driftSeconds < 5f ) {
				messager.enqueue( "FANTASTIC!\n+" + seconds, 1f, Type.Good, MessagePosition.Middle, MessageSize.Big );
			} else if( driftSeconds >= 5f ) {
				messager.enqueue( "UNREAL!\n+" + seconds, 1f, Type.Good, MessagePosition.Bottom, MessageSize.Big );
			}

			// epilogue, fade off drifting time label, slide in
			// the specified epilogue message
			if( playerDriftState.hasCollided ) {
				hudDrifting.endDrift( "-" + NumberString.format( driftSeconds ), EndDriftType.BadDrift );
			} else {
				hudDrifting.endDrift( "+" + NumberString.format( driftSeconds ), EndDriftType.GoodDrift );
			}

			break;
		}
	}

	//
	// MORE TASKS ??
	//

	private Vector2 offset = new Vector2();

	private void updateCarFriction() {
		Vector2 tilePosition = playerState.tilePosition;

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

			// bit twiddling, faster versions
			int xOnMap = (id & 3) * (int)gameWorld.map.tileWidth + (int)offset.x;
			int yOnMap = (id >> 2) * (int)gameWorld.map.tileWidth + (int)offset.y;

			int pixel = Art.frictionNature.getPixel( xOnMap, yOnMap );
			playerCar.setFriction( (pixel == -256 ? 0 : -1) );
		} else {
			Gdx.app.log( "GameLogic", "Out of map!" );
		}
	}

	// FIXME looks like this function is doing MUCH more than what's stated in its name..
	private void updateLap() {
		if( playerState.car != null ) {
			boolean onStartZone = (playerState.currTileX == gameWorld.playerStartTileX && playerState.currTileY == gameWorld.playerStartTileY);

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
						playerState.ghost.setReplay( any );
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
						playerState.ghost.setReplay( any );
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

						playerState.ghost.setReplay( best );

						lapState.restart();
						recorder.beginRecording( playerCar, worst, name, gameplaySettings.difficulty );
						lastRecordedLapId = worst.id;
					}
				}
			}
		}
	}
}
