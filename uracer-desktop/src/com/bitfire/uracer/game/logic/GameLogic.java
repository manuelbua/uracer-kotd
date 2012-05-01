package com.bitfire.uracer.game.logic;

import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.TweenEquation;
import aurelienribon.tweenengine.equations.Quad;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.g2d.tiled.TiledLayer;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.ScalingStrategy;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.configuration.Config;
import com.bitfire.uracer.game.GameplaySettings;
import com.bitfire.uracer.game.actors.Car;
import com.bitfire.uracer.game.actors.Car.Aspect;
import com.bitfire.uracer.game.actors.CarEvent;
import com.bitfire.uracer.game.actors.CarModel;
import com.bitfire.uracer.game.actors.CarState;
import com.bitfire.uracer.game.actors.CarStateEvent;
import com.bitfire.uracer.game.actors.GhostCar;
import com.bitfire.uracer.game.logic.hud.Hud;
import com.bitfire.uracer.game.logic.hud.HudLabel;
import com.bitfire.uracer.game.logic.hud.HudLabelAccessor;
import com.bitfire.uracer.game.logic.hud.debug.HudDebug;
import com.bitfire.uracer.game.logic.hud.elements.PlayerDriftInfo;
import com.bitfire.uracer.game.logic.hud.elements.PlayerDriftInfo.EndDriftType;
import com.bitfire.uracer.game.logic.hud.elements.PlayerLapTimes;
import com.bitfire.uracer.game.logic.notifier.Message;
import com.bitfire.uracer.game.logic.notifier.Message.Position;
import com.bitfire.uracer.game.logic.notifier.Message.Size;
import com.bitfire.uracer.game.logic.notifier.Message.Type;
import com.bitfire.uracer.game.logic.notifier.MessageAccessor;
import com.bitfire.uracer.game.logic.notifier.Messager;
import com.bitfire.uracer.game.logic.replaying.Replay;
import com.bitfire.uracer.game.logic.replaying.TrackLapManager;
import com.bitfire.uracer.game.logic.sounds.SoundEffect;
import com.bitfire.uracer.game.logic.sounds.SoundManager;
import com.bitfire.uracer.game.logic.sounds.effects.PlayerDriftSoundEffect;
import com.bitfire.uracer.game.logic.sounds.effects.PlayerImpactSoundEffect;
import com.bitfire.uracer.game.logic.trackeffects.TrackEffects;
import com.bitfire.uracer.game.logic.trackeffects.effects.PlayerSkidMarks;
import com.bitfire.uracer.game.player.PlayerCar;
import com.bitfire.uracer.game.player.PlayerDriftStateEvent;
import com.bitfire.uracer.game.rendering.GameRenderer;
import com.bitfire.uracer.game.rendering.GameWorldRenderer;
import com.bitfire.uracer.game.tween.GameTweener;
import com.bitfire.uracer.game.tween.WcTweener;
import com.bitfire.uracer.game.world.GameWorld;
import com.bitfire.uracer.game.world.models.WorldDefs.TileLayer;
import com.bitfire.uracer.resources.Art;
import com.bitfire.uracer.task.TaskManagerEvent;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.BoxedFloat;
import com.bitfire.uracer.utils.BoxedFloatAccessor;
import com.bitfire.uracer.utils.CarUtils;
import com.bitfire.uracer.utils.NumberString;

/** This concrete class manages to handle the inner game logic and its evolving states: it's all about gamedata, baby!
 * This should be refactored into smaller pieces of logic, that's where the components should came into.
 *
 * @author bmanuel */
public class GameLogic implements CarEvent.Listener, CarStateEvent.Listener, PlayerDriftStateEvent.Listener {
	// event
	// public final GameLogicEvent event = new GameLogicEvent();

	// scaling
	private ScalingStrategy scalingStrategy = null;

	// settings
	private GameplaySettings gameplaySettings = null;

	// world
	private GameWorld gameWorld = null;

	// input system
	private Input input = null;

	// physics step
	private PhysicsStep physicsStep;

	// player
	private boolean hasPlayer = false;
	private PlayerCar playerCar = null;
	private GhostCar ghostCar = null;

	// lap
	private TrackLapManager lapManager = null;
	private boolean isFirstLap = true;

	// tasks
	private GameTasksManager gameTasksManager = null;

	// special effects
	private TrackEffects effects = null;

	// hud
	private Hud hud = null;
	private PlayerDriftInfo hudPlayerDrift = null;

	// sound
	private SoundManager sound = null;

	// alerts and infos
	private Messager messager = null;

	// handles timeModulationBusy onComplete event
	private boolean timeModulation = false, timeModulationBusy = false;
	private BoxedFloat timeMultiplier = new BoxedFloat();
	public static final float TimeMultiplierMin = 0.3f;
	private TweenCallback timeModulationFinished = new TweenCallback() {
		@Override
		public void onEvent( int type, BaseTween<?> source ) {
			switch( type ) {
			case COMPLETE:
				timeModulationBusy = false;
			}
		}
	};

	public GameLogic( GameWorld gameWorld, GameplaySettings settings, ScalingStrategy scalingStrategy/* , Aspect
																									 * carAspect,
																									 * CarModel carModel */) {
		this.gameplaySettings = settings;
		this.scalingStrategy = scalingStrategy;
		this.gameWorld = gameWorld;

		// create tweening support
		Tween.registerAccessor( Message.class, new MessageAccessor() );
		Tween.registerAccessor( HudLabel.class, new HudLabelAccessor() );
		Tween.registerAccessor( BoxedFloat.class, new BoxedFloatAccessor() );
		Gdx.app.log( "GameLogic", "Tweening helpers created" );

		timeMultiplier.value = 1f;

		lapManager = new TrackLapManager( gameWorld, settings );

		// creates player and ghost cars
		ghostCar = CarFactory.createGhost( gameWorld, (new CarModel()).toDefault(), Aspect.OldSkool );
		// createPlayer( gameWorld, carAspect, carModel );
		// Gdx.app.log( "GameLogic", "Player created" );

		// configurePlayer( gameWorld, gameplaySettings, playerCar );
		// Gdx.app.log( "GameLogic", "Player configured" );

		createGameTasks( gameWorld, scalingStrategy );
		// configurePlayerTasks( playerCar, playerLapState );
		// Gdx.app.log( "GameLogic", "Game tasks created and configured" );

		// subscribe to player-related events
		// registerPlayerEvents( playerCar );
		// Gdx.app.log( "GameLogic", "Registered player-related events" );

		// messager.show( "COOL STUFF!", 60, Message.Type.Information, MessagePosition.Bottom, MessageSize.Big );
	}

	public void dispose() {
		gameTasksManager.dispose();

		if( playerCar != null ) {
			playerCar.dispose();
		}

		if( ghostCar != null ) {
			ghostCar.dispose();
		}

		gameWorld.dispose();
		GameTweener.dispose();
		WcTweener.dispose();
	}

	/** Sets the player and transfer ownership to the GameLogic object */
	public void setPlayer( PlayerCar player ) {
		this.playerCar = player;

		if( player != null ) {
			configurePlayer( gameWorld, gameplaySettings, player );
			Gdx.app.log( "GameLogic", "Player configured" );

			configurePlayerTasks( player, lapManager.getLapInfo() );
			Gdx.app.log( "GameLogic", "Game tasks created and configured" );

			registerPlayerEvents( player );
			Gdx.app.log( "GameLogic", "Registered player-related events" );

			hasPlayer = true;
		} else {
			hasPlayer = false;
		}
	}

	public void setBestLocalReplay( Replay replay ) {
		lapManager.setBestReplay( replay );
		restartGame();
		if( !hasPlayer ) {
			ghostCar.setReplay( replay );
		}
	}

	private void registerPlayerEvents( PlayerCar player ) {
		player.carState.event.addListener( this, CarStateEvent.Type.onTileChanged );
		player.driftState.event.addListener( this, PlayerDriftStateEvent.Type.onBeginDrift );
		player.driftState.event.addListener( this, PlayerDriftStateEvent.Type.onEndDrift );
		player.event.addListener( this, CarEvent.Type.onCollision );
		player.event.addListener( this, CarEvent.Type.onComputeForces );
	}

	private void createGameTasks( GameWorld gameWorld, ScalingStrategy strategy ) {
		gameTasksManager = new GameTasksManager();

		// input system
		input = new Input( TaskManagerEvent.Order.MINUS_4 );

		// physics step
		physicsStep = new PhysicsStep( gameWorld.getBox2DWorld(), TaskManagerEvent.Order.MINUS_3 );

		// sound manager
		sound = new SoundManager();
		gameTasksManager.add( sound );

		// message manager
		messager = new Messager( strategy.invTileMapZoomFactor );
		gameTasksManager.add( messager );

		// hud manager
		hud = new Hud();
		gameTasksManager.add( hud );

		// effects manager
		effects = new TrackEffects();
		gameTasksManager.add( effects );
	}

	private void configurePlayerTasks( PlayerCar player, TrackLapInfo lapState ) {
		// sounds
		SoundEffect fx = new PlayerDriftSoundEffect( player );
		fx.start();
		sound.add( fx );
		sound.add( new PlayerImpactSoundEffect( player ) );

		// track effects
		PlayerSkidMarks carSkidMarks = new PlayerSkidMarks( player );
		effects.add( carSkidMarks );

		// hud, player's drift information
		hudPlayerDrift = new PlayerDriftInfo( scalingStrategy, player );
		hud.add( hudPlayerDrift );

		// hud, player's lap times
		PlayerLapTimes lapTimes = new PlayerLapTimes( scalingStrategy, lapState );
		hud.add( lapTimes );

		// hud-style debug information for various data (player's drift state, number of skid marks particles, ..)
		if( Config.Debug.RenderHudDebugInfo ) {
			HudDebug hudDebug = new HudDebug( player, player.driftState, carSkidMarks );
			hud.add( hudDebug );
		}
	}

	// private void createPlayer( GameWorld gameWorld, Aspect carAspect, CarModel carModel ) {
	// playerCar = CarFactory.createPlayer( gameWorld, carAspect, carModel );
	// // ghostCar = CarFactory.createGhost( gameWorld, playerCar );
	// }

	private void configurePlayer( GameWorld world, GameplaySettings settings, PlayerCar player ) {
		// create player and setup player input system and initial position in the world
		// player.setInputSystem( input );
		player.setTransform( world.playerStartPos, world.playerStartOrient );

		// apply handicaps
		player.setLinearVelocityDampingAF( settings.linearVelocityDampingAfterFeedback );
		player.setThrottleDampingAF( settings.throttleDampingAfterFeedback );
	}

	public GameWorld getGameWorld() {
		return gameWorld;
	}

	public PlayerCar getPlayer() {
		return playerCar;
	}

	public boolean onTick() {
		if( input.isOn( Keys.R ) ) {
			restartGame();
		} else if( input.isOn( Keys.T ) ) {
			resetGame();
		} else if( input.isOn( Keys.Q ) ) {
			Gdx.app.exit();
			return false;
		} else if( input.isOn( Keys.SPACE ) && !timeModulationBusy ) {

			TweenEquation eqIn = Quad.OUT;
			TweenEquation eqOut = Quad.INOUT;

			timeModulation = !timeModulation;
			if( timeModulation ) {
				timeModulationBusy = true;
				WcTweener.start( Timeline.createSequence().push( Tween.to( timeMultiplier, BoxedFloatAccessor.VALUE, 1000 ).target( TimeMultiplierMin ).ease( eqIn ) )
						.setCallback( timeModulationFinished ) );
			} else {
				timeModulationBusy = true;
				WcTweener.start( Timeline.createSequence()
						.push( Tween.to( timeMultiplier, BoxedFloatAccessor.VALUE, 1000 ).target( Config.Physics.PhysicsTimeMultiplier ).ease( eqOut ) )
						.setCallback( timeModulationFinished ) );
			}
		}

		if( playerCar != null ) {
			updatePlayerCarFriction();
		}

		updateTimeMultiplier();

		return true;
	}

	private void updateTimeMultiplier() {
		URacer.timeMultiplier = AMath.clamp( timeMultiplier.value, TimeMultiplierMin, Config.Physics.PhysicsTimeMultiplier );
	}

	//
	// RENDERING LOGIC
	//

	public void onBeforeRender( GameRenderer gameRenderer ) {
		// trigger the event and let's subscribers interpolate and update their state()
		physicsStep.triggerOnTemporalAliasing( URacer.hasStepped(), URacer.getTemporalAliasing() );

		// update player's headlights and move the world camera to follows it, if there is a player
		GameWorldRenderer worldRenderer = gameRenderer.getWorldRenderer();
		if( hasPlayer ) {

			if( gameWorld.isNightMode() ) {
				worldRenderer.updatePlayerHeadlights( playerCar );
			}

			worldRenderer.setCameraPosition( playerCar.state().position, true );
		} else if( ghostCar.hasReplay() ) {
			worldRenderer.setCameraPosition( ghostCar.state().position, true );
		} else {
			// no ghost, no player, WTF?
			worldRenderer.setCameraPosition( gameWorld.playerStartPos, true );
		}

		// tweener step
		WcTweener.update();
		GameTweener.update();
	}

	//
	// TODO, COULD THIS BE A TASK HANDLING IN-GAME USER CHOICES ??
	//

	public void restartGame() {
		restartLogic();
		gameTasksManager.restart();
	}

	public void resetGame() {
		restartLogic();
		resetLogic();
		gameTasksManager.reset();
	}

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
		resetPlayer( playerCar, ghostCar );
		isFirstLap = true;
		timeModulationBusy = false;
		timeModulation = false;
		timeMultiplier.value = Config.Physics.PhysicsTimeMultiplier;
		WcTweener.clear();
		GameTweener.clear();
		lapManager.abortRecording();
		gameTasksManager.restart();
	}

	private void resetLogic() {
		lapManager.reset();
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
			lapManager.onPlayerComputeForces( data.forces );
			break;
		}
	};

	@Override
	public void carStateEvent( CarState source, CarStateEvent.Type type ) {
		switch( type ) {
		case onTileChanged:
			playerTileChanged();
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
			hudPlayerDrift.beginDrift();
			break;
		case onEndDrift:
			String seconds = NumberString.format( player.driftState.driftSeconds() ) + "  seconds!";
			boolean driftEndByCollision = player.driftState.hasCollided;
			float driftSeconds = player.driftState.driftSeconds();

			if( !driftEndByCollision ) {
				if( driftSeconds >= 1 && driftSeconds < 3f ) {
					messager.enqueue( "NICE ONE!\n+" + seconds, 1f, Type.Good, Position.Middle, Size.Big );
				} else if( driftSeconds >= 3f && driftSeconds < 5f ) {
					messager.enqueue( "FANTASTIC!\n+" + seconds, 1f, Type.Good, Position.Middle, Size.Big );
				} else if( driftSeconds >= 5f ) {
					messager.enqueue( "UNREAL!\n+" + seconds, 1f, Type.Good, Position.Bottom, Size.Big );
				}

				hudPlayerDrift.endDrift( "+" + NumberString.format( driftSeconds ), EndDriftType.GoodDrift );

			} else {
				hudPlayerDrift.endDrift( "-" + NumberString.format( driftSeconds ), EndDriftType.BadDrift );
			}

			break;
		}

		// Gdx.app.log( "GameLogic", "playerDriftStateEvent::ds=" + NumberString.format(
		// player.driftState.driftSeconds() ) + " (" + player.driftState.driftSeconds() + ")" );
	}

	//
	// MORE TASKS ??
	//

	private Vector2 offset = new Vector2();

	private void updatePlayerCarFriction() {
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

	private void playerTileChanged() {
		boolean onStartZone = (playerCar.carState.currTileX == gameWorld.playerStartTileX && playerCar.carState.currTileY == gameWorld.playerStartTileY);

		if( onStartZone ) {
			if( isFirstLap ) {
				isFirstLap = false;

				// any record to play?
				if( lapManager.hasAnyReplay() ) {
					ghostCar.setReplay( lapManager.getAnyReplay() );
				}

				lapManager.startRecording( playerCar );

			} else {
				lapManager.stopRecording();

				if( !lapManager.hasAllReplays() ) {
					// only one single valid replay

					Replay any = lapManager.getAnyReplay();
					ghostCar.setReplay( any );
					any.saveLocal( messager );
					messager.show( "GO!  GO!  GO!", 3f, Type.Information, Position.Middle, Size.Big );

				} else {

					// both valid, replay best, overwrite worst

					Replay best = lapManager.getBestReplay();
					Replay worst = lapManager.getWorstReplay();

					float bestTime = AMath.round( best.trackTimeSeconds, 2 );
					float worstTime = AMath.round( worst.trackTimeSeconds, 2 );
					float diffTime = AMath.round( worstTime - bestTime, 2 );

					if( AMath.equals( worstTime, bestTime ) ) {
						// draw!
						messager.show( "DRAW!", 3f, Type.Information, Position.Top, Size.Big );
					} else {
						// has the player managed to beat the best lap?
						if( lapManager.isLastBestLap() ) {
							messager.show( "-" + NumberString.format( diffTime ) + " seconds!", 3f, Type.Good, Position.Top, Size.Big );
						} else {
							messager.show( "+" + NumberString.format( diffTime ) + " seconds", 3f, Type.Bad, Position.Top, Size.Big );
						}
					}

					ghostCar.setReplay( best );
					best.saveLocal( messager );
				}

				CarUtils.dumpSpeedInfo( "Player", playerCar, lapManager.getLastRecordedReplay().trackTimeSeconds );

				lapManager.startRecording( playerCar );
			}

			playerCar.resetTraveledDistance();
		}
	}
}
