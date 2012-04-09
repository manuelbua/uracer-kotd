package com.bitfire.uracer.game;

import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.TweenEquation;
import aurelienribon.tweenengine.equations.Sine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.carsimulation.CarInputMode;
import com.bitfire.uracer.carsimulation.Recorder;
import com.bitfire.uracer.carsimulation.Replay;
import com.bitfire.uracer.events.CarEvent;
import com.bitfire.uracer.events.GameLogicEvent;
import com.bitfire.uracer.events.PlayerStateEvent;
import com.bitfire.uracer.game.GameData.Events;
import com.bitfire.uracer.game.GameData.States;
import com.bitfire.uracer.game.logic.DirectorController;
import com.bitfire.uracer.game.logic.LapState;
import com.bitfire.uracer.game.logic.PlayerState;
import com.bitfire.uracer.messager.Messager.MessagePosition;
import com.bitfire.uracer.messager.Messager.MessageSize;
import com.bitfire.uracer.messager.Messager.MessageType;
import com.bitfire.uracer.tweener.BoxedFloat;
import com.bitfire.uracer.tweener.accessors.BoxedFloatAccessor;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.NumberString;

public class GameLogic implements CarEvent.Listener, PlayerStateEvent.Listener {
	// lap
	private boolean isFirstLap = true;
	private long lastRecordedLapId = 0;

	//
	private DirectorController controller = null;

	// replay
	private Recorder recorder = null;

	public GameLogic() {
		Events.gameLogic.source = this;
		recorder = new Recorder();
		timeMultiplier.value = 1f;

		Events.playerState.addListener( this );
		Events.carEvent.addListener( this );
		States.playerState.car.setTransform( GameData.gameWorld.playerStartPos, GameData.gameWorld.playerStartOrient );

		controller = new DirectorController( Config.Graphics.CameraInterpolationMode, Director.halfViewport, GameData.gameWorld.worldSizeScaledPx,
				GameData.gameWorld.worldSizeTiles );
	}

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

	public boolean onTick() {
		if( Input.isOn( Keys.R ) ) {
			restart();
			Events.gameLogic.trigger( GameLogicEvent.Type.onRestart );
		} else if( Input.isOn( Keys.T ) ) {
			restart();
			reset();
			Events.gameLogic.trigger( GameLogicEvent.Type.onReset );
		} else if( Input.isOn( Keys.Q ) ) {
			Gdx.app.exit();
			return false;
		} else if( Input.isOn( Keys.SPACE ) ) {
			if( !timeModulationBusy ) {

				TweenEquation eqIn = Sine.INOUT;
				TweenEquation eqOut = Sine.INOUT;

				timeModulation = !timeModulation;
				if( timeModulation ) {
					timeModulationBusy = true;
					GameData.tweener.start( Timeline.createSequence().push( Tween.to( timeMultiplier, BoxedFloatAccessor.VALUE, 1000 ).target( tmMin ).ease( eqIn ) )
							.setCallback( timeModulationFinished ) );
				} else {
					timeModulationBusy = true;
					GameData.tweener.start( Timeline.createSequence()
							.push( Tween.to( timeMultiplier, BoxedFloatAccessor.VALUE, 1000 ).target( Config.Physics.PhysicsTimeMultiplier ).ease( eqOut ) )
							.setCallback( timeModulationFinished ) );
				}
			}
		}

		URacer.timeMultiplier = AMath.clamp( timeMultiplier.value, tmMin, Config.Physics.PhysicsTimeMultiplier );

		return true;
	}

	public void onBeforeRender() {
		GameData.Systems.physicsStep.triggerOnTemporalAliasing( URacer.getTemporalAliasing() );

		// follow the player's car
		if( GameData.States.playerState != null && GameData.States.playerState.car != null ) {
			controller.setPosition( GameData.States.playerState.car.state().position );
		}
	}

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

	@Override
	public void carEvent( CarEvent.Type type, CarEvent.Data data ) {
		switch( type ) {
		case onCollision:
			if( data.car.getInputMode() == CarInputMode.InputFromPlayer ) {
				if( GameData.States.driftState.isDrifting ) {
					GameData.States.driftState.invalidateByCollision();
					// System.out.println( "invalidated" );
				}
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
			PlayerState player = GameData.States.playerState;
			boolean onStartZone = (player.currTileX == GameData.gameWorld.playerStartTileX && player.currTileY == GameData.gameWorld.playerStartTileY);

			LapState lapState = GameData.States.lapState;
			String name = GameData.gameWorld.name;

			if( onStartZone ) {
				if( isFirstLap ) {
					isFirstLap = false;

					lapState.restart();
					Replay buf = lapState.getNextBuffer();
					recorder.beginRecording( player.car, buf, /* lapState.getStartNanotime(), */name );
					lastRecordedLapId = buf.id;

					if( lapState.hasAnyReplayData() ) {
						Replay any = lapState.getAnyReplay();
						player.ghost.setReplay( any );
					}
				} else {
					if( recorder.isRecording() )
						recorder.endRecording();

					lapState.updateReplays();

					// replay best, overwrite worst logic

					if( !lapState.hasAllReplayData() ) {
						// only one single replay
						lapState.restart();
						Replay buf = lapState.getNextBuffer();
						recorder.beginRecording( player.car, buf, /* lapState.getStartNanotime(), */name );
						lastRecordedLapId = buf.id;

						Replay any = lapState.getAnyReplay();
						player.ghost.setReplay( any );
						lapState.setLastTrackTimeSeconds( any.trackTimeSeconds );

						GameData.messager.show( "GO!  GO!  GO!", 3f, MessageType.Information, MessagePosition.Middle, MessageSize.Big );
					} else {
						// both valid, replay best, overwrite worst
						Replay best = lapState.getBestReplay(), worst = lapState.getWorstReplay();

						if( lastRecordedLapId == best.id ) {
							lapState.setLastTrackTimeSeconds( best.trackTimeSeconds );
							GameData.messager.show( "-" + NumberString.format( worst.trackTimeSeconds - best.trackTimeSeconds ) + " seconds!", 3f, MessageType.Good,
									MessagePosition.Top, MessageSize.Big );
						} else {
							lapState.setLastTrackTimeSeconds( worst.trackTimeSeconds );
							GameData.messager.show( "+" + NumberString.format( worst.trackTimeSeconds - best.trackTimeSeconds ) + " seconds", 3f, MessageType.Bad,
									MessagePosition.Top, MessageSize.Big );
						}

						player.ghost.setReplay( best );

						lapState.restart();
						recorder.beginRecording( player.car, worst, /* lapState.getStartNanotime(), */name );
						lastRecordedLapId = worst.id;
					}
				}
			}
			break;
		}
	}
}
