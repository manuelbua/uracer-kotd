package com.bitfire.uracer.game;

import aurelienribon.tweenengine.BaseTween;
import aurelienribon.tweenengine.Timeline;
import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenCallback;
import aurelienribon.tweenengine.TweenEquation;
import aurelienribon.tweenengine.equations.Cubic;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.URacer;
import com.bitfire.uracer.carsimulation.CarForces;
import com.bitfire.uracer.carsimulation.CarInputMode;
import com.bitfire.uracer.carsimulation.Recorder;
import com.bitfire.uracer.carsimulation.Replay;
import com.bitfire.uracer.effects.TrackEffects;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.events.CarListener;
import com.bitfire.uracer.events.GameLogicEvent;
import com.bitfire.uracer.events.GameLogicEvent.EventType;
import com.bitfire.uracer.events.PlayerStateListener;
import com.bitfire.uracer.game.logic.LapState;
import com.bitfire.uracer.game.logic.PlayerState;
import com.bitfire.uracer.messager.Messager;
import com.bitfire.uracer.messager.Messager.MessagePosition;
import com.bitfire.uracer.messager.Messager.MessageSize;
import com.bitfire.uracer.messager.Messager.MessageType;
import com.bitfire.uracer.tweener.BoxedFloat;
import com.bitfire.uracer.tweener.accessors.BoxedFloatAccessor;
import com.bitfire.uracer.utils.AMath;
import com.bitfire.uracer.utils.NumberString;

public class GameLogic implements CarListener, PlayerStateListener {
	// lap
	private boolean isFirstLap = true;
	private long lastRecordedLapId = 0;

	// replay
	private Recorder recorder = null;

	// private GameLogicNotifier notifier = null;

	public static final GameLogicEvent event = new GameLogicEvent();

	public GameLogic() {
		this.recorder = new Recorder();
		// this.notifier = new GameLogicNotifier();
		timeMultiplier.value = 1f;
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
		EntityManager.raiseOnTick( GameData.world );

		if( Input.isOn( Keys.R ) ) {
			restart();
			event.trigger( EventType.OnRestart );
		} else if( Input.isOn( Keys.T ) ) {
			restart();
			reset();
			event.trigger( EventType.OnReset );
		} else if( Input.isOn( Keys.Q ) ) {
			Gdx.app.exit();
			return false;
		} else if( Input.isOn( Keys.SPACE ) ) {
			if( !timeModulationBusy ) {

				TweenEquation eqIn = Cubic.INOUT;
				TweenEquation eqOut = Cubic.INOUT;

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

		// System.out.println( timeModulationBusy + " - " + timeMultiplier.value );

		// if( timeModulation ) {
		// URacer.timeMultiplier = AMath.clamp( URacer.timeMultiplier - 0.02f, tmMin,
		// Config.Physics.PhysicsTimeMultiplier );
		// }
		// else {
		// URacer.timeMultiplier = AMath.clamp( URacer.timeMultiplier + 0.02f, tmMin,
		// Config.Physics.PhysicsTimeMultiplier );
		// }

		GameData.playerState.tick();
		GameData.driftState.tick();
		GameData.lapState.tick();

		GameData.hud.tick();
		TrackEffects.tick();

		recorder.tick();

		return true;
	}

	private void restart() {
		GameData.tweener.clear();
		GameData.driftState.reset();
		GameData.hud.reset();
		Messager.reset();
		TrackEffects.reset();
		recorder.reset();
		GameData.playerState.reset();
		isFirstLap = true;
		timeModulationBusy = false;
		timeModulation = false;
		timeMultiplier.value = Config.Physics.PhysicsTimeMultiplier;
	}

	private void reset() {
		restart();
		GameData.lapState.reset();
		lastRecordedLapId = 0;
	}

	// ----------------------------------------------------------------------
	//
	// from CarListener
	//
	// ----------------------------------------------------------------------

	@Override
	public void onCollision( Car car, Fixture other, Vector2 impulses ) {
		if( car.getInputMode() == CarInputMode.InputFromPlayer ) {
			if( GameData.driftState.isDrifting ) {
				GameData.driftState.invalidateByCollision();
				// System.out.println( "invalidated" );
			}
		}
	}

	@Override
	public void onComputeForces( CarForces forces ) {
		if( recorder.isRecording() ) {
			recorder.add( forces );
		}
	}

	// ----------------------------------------------------------------------
	//
	// from PlayerStateListener
	//
	// ----------------------------------------------------------------------

	@Override
	public void onTileChanged() {
		PlayerState player = GameData.playerState;
		boolean onStartZone = (player.currTileX == player.startTileX && player.currTileY == player.startTileY);

		LapState lapState = GameData.lapState;
		String name = GameData.level.name;

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

					Messager.show( "GO!  GO!  GO!", 3f, MessageType.Information, MessagePosition.Middle, MessageSize.Big );
				} else {
					// both valid, replay best, overwrite worst
					Replay best = lapState.getBestReplay(), worst = lapState.getWorstReplay();

					if( lastRecordedLapId == best.id ) {
						lapState.setLastTrackTimeSeconds( best.trackTimeSeconds );
						Messager.show( "-" + NumberString.format( worst.trackTimeSeconds - best.trackTimeSeconds ) + " seconds!", 3f, MessageType.Good,
								MessagePosition.Top, MessageSize.Big );
					} else {
						lapState.setLastTrackTimeSeconds( worst.trackTimeSeconds );
						Messager.show( "+" + NumberString.format( worst.trackTimeSeconds - best.trackTimeSeconds ) + " seconds", 3f, MessageType.Bad,
								MessagePosition.Top, MessageSize.Big );
					}

					player.ghost.setReplay( best );

					lapState.restart();
					recorder.beginRecording( player.car, worst, /* lapState.getStartNanotime(), */name );
					lastRecordedLapId = worst.id;
				}
			}
		}
	}
}
