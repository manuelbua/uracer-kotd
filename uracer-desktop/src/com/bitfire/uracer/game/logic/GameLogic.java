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
import com.bitfire.uracer.Art;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.game.GameEvents;
import com.bitfire.uracer.game.Input;
import com.bitfire.uracer.game.Replay;
import com.bitfire.uracer.game.audio.CarDriftSoundEffect;
import com.bitfire.uracer.game.audio.CarImpactSoundEffect;
import com.bitfire.uracer.game.data.GameData;
import com.bitfire.uracer.game.effects.CarSkidMarks;
import com.bitfire.uracer.game.effects.TrackEffects;
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
import com.bitfire.uracer.game.messager.Message;
import com.bitfire.uracer.game.messager.Message.MessagePosition;
import com.bitfire.uracer.game.messager.Message.MessageSize;
import com.bitfire.uracer.game.messager.Message.Type;
import com.bitfire.uracer.game.messager.MessageAccessor;
import com.bitfire.uracer.game.messager.Messager;
import com.bitfire.uracer.game.player.Car;
import com.bitfire.uracer.game.player.Car.InputMode;
import com.bitfire.uracer.game.player.CarEvent;
import com.bitfire.uracer.game.player.CarModel;
import com.bitfire.uracer.game.states.DriftState;
import com.bitfire.uracer.game.states.LapState;
import com.bitfire.uracer.game.states.PlayerState;
import com.bitfire.uracer.game.tween.GameTweener;
import com.bitfire.uracer.game.tween.WcTweener;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.game.world.WorldDefs.TileLayer;
import com.bitfire.uracer.postprocessing.PostProcessor;
import com.bitfire.uracer.postprocessing.effects.Bloom;
import com.bitfire.uracer.postprocessing.effects.Zoom;
import com.bitfire.uracer.task.Task;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.BoxedFloat;
import com.bitfire.uracer.utils.BoxedFloatAccessor;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.NumberString;

// TODO, GameTasks entity for managing them with get(name)/get(id)? Opening up to Components interacting with each
// other? I don't quite like that..
public class GameLogic implements CarEvent.Listener, PlayerStateEvent.Listener, DriftStateEvent.Listener {
	// lap
	private boolean isFirstLap = true;
	private long lastRecordedLapId = 0;

	private DirectorController controller = null;

	// post-processing
	private PostProcessor postProcessor;
	private Bloom bloom = null;
	private Zoom zoom = null;

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

	public GameLogic( PostProcessor postProcessor ) {
		// create tweening support
		Tween.registerAccessor( Message.class, new MessageAccessor() );
		Tween.registerAccessor( HudLabel.class, new HudLabelAccessor() );
		Tween.registerAccessor( BoxedFloat.class, new BoxedFloatAccessor() );

		// wall-clocked tweener, use this to tween the timeMultiplier
		WcTweener.init();

		// game tweener, for all the rest
		GameTweener.init();

		this.postProcessor = postProcessor;

		recorder = new Recorder();
		timeMultiplier.value = 1f;

		GameEvents.playerState.addListener( this );
		GameEvents.carEvent.addListener( this );
		GameEvents.playerDriftState.addListener( this );

		GameWorld world = GameData.Environment.gameWorld;

		// initialize player position in the world
		GameData.States.player.car.setTransform( world.playerStartPos, world.playerStartOrient );

		// creates global camera controller
		controller = new DirectorController( Config.Graphics.CameraInterpolationMode, Director.halfViewport, world.worldSizeScaledPx, world.worldSizeTiles );

		createGameTasks();
		setupPostProcessing();

		messager.show( "COOL STUFF!", 60, Message.Type.Information, MessagePosition.Bottom, MessageSize.Big );
	}

	public void dispose() {
		for( Task task : gameTasks ) {
			task.dispose();
		}

		gameTasks.clear();
	}

	private void setupPostProcessing() {
		if( !Config.Graphics.EnablePostProcessingFx || postProcessor == null ) {
			return;
		}

		bloom = new Bloom( Config.PostProcessing.RttFboWidth, Config.PostProcessing.RttFboHeight );

		// Bloom.Settings bs = new Bloom.Settings( "arrogance-1 / rtt=0.25 / @1920x1050", BlurType.Gaussian5x5b, 1, 1,
		// 0.25f, 1f, 0.1f, 0.8f, 1.4f );
		// Bloom.Settings bs = new Bloom.Settings( "arrogance-2 / rtt=0.25 / @1920x1050", BlurType.Gaussian5x5b, 1, 1,
		// 0.35f, 1f, 0.1f, 1.4f, 0.75f );

		float threshold = ((GameData.Environment.gameWorld.isNightMode() && !Config.Graphics.DumbNightMode) ? 0.2f : 0.45f);
		Bloom.Settings bloomSettings = new Bloom.Settings( "subtle", Config.PostProcessing.BlurType, 1, 1.5f, threshold, 1f, 0.5f, 1f, 1.5f );
		bloom.setSettings( bloomSettings );

		zoom = new Zoom( Config.PostProcessing.ZoomQuality );
		postProcessor.addEffect( zoom );

		postProcessor.addEffect( bloom );
	}

	private void createGameTasks() {
		gameTasks = new ArrayList<Task>( 10 );

		sound = new SoundManager();
		gameTasks.add( sound );

		messager = new Messager( GameData.Environment.scalingStrategy.invTileMapZoomFactor );
		gameTasks.add( messager );

		hud = new Hud();
		gameTasks.add( hud );

		effects = new TrackEffects();
		gameTasks.add( effects );

		// configure effects
		configureTasks();
	}

	private void configureTasks() {
		Car car = GameData.States.player.car;
		CarModel model = car.getCarModel();
		float carModelWithPx = Convert.mt2px( model.width );
		float carModelLengthPx = Convert.mt2px( model.length );

		// sounds
		ISoundEffect fx = new CarDriftSoundEffect();
		fx.start();
		sound.add( fx );
		sound.add( new CarImpactSoundEffect() );

		// track effects
		playerSkidMarks = new CarSkidMarks( carModelWithPx, carModelLengthPx );
		effects.add( playerSkidMarks );

		// hud
		hudDrifting = new HudDrifting( carModelWithPx, carModelLengthPx );
		hud.add( hudDrifting );
	}

	public boolean onTick() {
		Input input = GameData.Systems.input;

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

		updatePostProcessing();

		return true;
	}

	private void updateGameTasks() {
		updateHud();
		updateTrackEffects();
	}

	private void updateTrackEffects() {
		// update track effects
		Car player = GameData.States.player.car;
		if( player.getCarDescriptor().velocity_wc.len2() >= 1 ) {
			playerSkidMarks.tryAddDriftMark( player.state().position, player.state().orientation, GameData.States.playerDrift );
		}
	}

	private void updateStates() {
		GameData.States.player.update();

		// compute drift state for player's car
		GameData.States.playerDrift.update( GameData.States.player.car );
	}

	private void updateTimeMultiplier() {
		URacer.timeMultiplier = AMath.clamp( timeMultiplier.value, tmMin, Config.Physics.PhysicsTimeMultiplier );
	}

	private void updateHud() {
		hud.update( GameData.States.lap );
		hudDrifting.update( GameData.States.playerDrift );
	}

	private void updatePostProcessing() {
		float factor = 1 - (URacer.timeMultiplier - 0.3f) / (Config.Physics.PhysicsTimeMultiplier - 0.3f);

		if( Config.Graphics.EnablePostProcessingFx && zoom != null ) {
			zoom.setOrigin( Director.screenPosFor( GameData.States.player.car.getBody() ) );
			zoom.setStrength( -0.05f * factor );
		}

		if( Config.Graphics.EnablePostProcessingFx && bloom != null && zoom != null ) {
			bloom.setBaseSaturation( 0.5f - 0.5f * factor );
			bloom.setBloomSaturation( 1.5f - factor * 1.15f );
			bloom.setBloomIntesity( 1f + factor * 1.75f );
		}
	}

	//
	// RENDERING-BOUND LOGIC
	//

	public void onBeforeRender() {
		// trigger the event and let's subscribers interpolate and update their state()
		GameData.Systems.physicsStep.triggerOnTemporalAliasing( URacer.getTemporalAliasing() );

		Car playerCar = GameData.States.player.car;
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

	private void restart() {
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
			DriftState driftState = GameData.States.playerDrift;
			Car car = GameEvents.carEvent.data.car;
			if( car.getInputMode() == InputMode.InputFromPlayer && driftState.isDrifting ) {
				driftState.invalidateByCollision();
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
			DriftState drift = GameData.States.playerDrift;
			String seconds = NumberString.format( drift.driftSeconds() ) + "  seconds!";

			float driftSeconds = drift.driftSeconds();
			if( driftSeconds >= 1 && driftSeconds < 3f ) {
				messager.enqueue( "NICE ONE!\n+" + seconds, 1f, Type.Good, MessagePosition.Middle, MessageSize.Big );
			} else if( driftSeconds >= 3f && driftSeconds < 5f ) {
				messager.enqueue( "FANTASTIC!\n+" + seconds, 1f, Type.Good, MessagePosition.Middle, MessageSize.Big );
			} else if( driftSeconds >= 5f ) {
				messager.enqueue( "UNREAL!\n+" + seconds, 1f, Type.Good, MessagePosition.Bottom, MessageSize.Big );
			}

			// epilogue, fade off drifting time label, slide in
			// the specified epilogue message
			if( drift.hasCollided ) {
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
		PlayerState player = GameData.States.player;

		Vector2 tilePosition = player.tilePosition;

		GameWorld world = GameData.Environment.gameWorld;
		if( world.isValidTilePosition( tilePosition ) ) {
			// compute realsize-based pixel offset car-tile (top-left origin)
			float scaledTileSize = GameData.Environment.gameWorld.getTileSizeScaled();
			float tsx = tilePosition.x * scaledTileSize;
			float tsy = tilePosition.y * scaledTileSize;
			offset.set( player.car.state().position );
			offset.y = world.worldSizeScaledPx.y - offset.y;
			offset.x = offset.x - tsx;
			offset.y = offset.y - tsy;
			offset.mul( GameData.Environment.gameWorld.getTileSizeInvScaled() ).mul( world.map.tileWidth );

			TiledLayer layerTrack = GameData.Environment.gameWorld.getLayer( TileLayer.Track );
			int id = layerTrack.tiles[(int)tilePosition.y][(int)tilePosition.x] - 1;

			// int xOnMap = (id %4) * 224 + (int)offset.x;
			// int yOnMap = (int)( id/4f ) * 224 + (int)offset.y;

			// bit twiddling, faster versions
			int xOnMap = (id & 3) * (int)world.map.tileWidth + (int)offset.x;
			int yOnMap = (id >> 2) * (int)world.map.tileWidth + (int)offset.y;

			int pixel = Art.frictionNature.getPixel( xOnMap, yOnMap );
			player.car.setFriction( (pixel == -256 ? 0 : -1) );
		} else {
			Gdx.app.log( "GameLogic", "Out of map!" );
		}
	}

	// FIXME looks like this function is doing MUCH more than what's stated in its name..
	private void updateLap() {
		PlayerState player = GameData.States.player;
		if( player.car != null ) {
			GameWorld world = GameData.Environment.gameWorld;

			boolean onStartZone = (player.currTileX == world.playerStartTileX && player.currTileY == world.playerStartTileY);

			LapState lapState = GameData.States.lap;
			String name = world.name;

			if( onStartZone ) {
				if( isFirstLap ) {
					isFirstLap = false;

					lapState.restart();
					Replay buf = lapState.getNextBuffer();
					recorder.beginRecording( player.car, buf, name );
					lastRecordedLapId = buf.id;

					if( lapState.hasAnyReplayData() ) {
						Replay any = lapState.getAnyReplay();
						player.ghost.setReplay( any );
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
						recorder.beginRecording( player.car, buf, name );
						lastRecordedLapId = buf.id;

						Replay any = lapState.getAnyReplay();
						player.ghost.setReplay( any );
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

						player.ghost.setReplay( best );

						lapState.restart();
						recorder.beginRecording( player.car, worst, name );
						lastRecordedLapId = worst.id;
					}
				}
			}
		}
	}
}
