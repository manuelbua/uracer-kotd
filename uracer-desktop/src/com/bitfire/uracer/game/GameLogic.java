package com.bitfire.uracer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.audio.CarSoundManager;
import com.bitfire.uracer.carsimulation.CarForces;
import com.bitfire.uracer.carsimulation.CarInputMode;
import com.bitfire.uracer.carsimulation.Recorder;
import com.bitfire.uracer.carsimulation.Replay;
import com.bitfire.uracer.effects.TrackEffects;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.events.CarListener;
import com.bitfire.uracer.events.DriftStateListener;
import com.bitfire.uracer.events.PlayerStateListener;
import com.bitfire.uracer.game.logic.LapState;
import com.bitfire.uracer.game.logic.PlayerState;
import com.bitfire.uracer.messager.Messager;
import com.bitfire.uracer.messager.Messager.MessagePosition;
import com.bitfire.uracer.messager.Messager.MessageSize;
import com.bitfire.uracer.messager.Messager.MessageType;
import com.bitfire.uracer.utils.NumberString;

public class GameLogic implements CarListener, DriftStateListener, PlayerStateListener {
	// lap
	private boolean isFirstLap = true;
	private long lastRecordedLapId = 0;

	// replay
	private Recorder recorder = null;

	public GameLogic() {
		this.recorder = new Recorder();
	}

	public boolean onTick() {
		EntityManager.raiseOnTick( GameData.world );

		if( Input.isOn( Keys.R ) ) {
			restart();
		}
		else if( Input.isOn( Keys.T ) ) {
			restart();
			reset();
		}
		else if( Input.isOn( Keys.Q ) ) {
			Gdx.app.exit();
			return false;
		}

		GameData.playerState.tick();
		GameData.driftState.tick();

		GameData.hud.tick();
		TrackEffects.tick();
		CarSoundManager.tick();

		return true;
	}

	private void restart() {
		CarSoundManager.reset();
		GameData.tweener.clear();
		GameData.driftState.reset();
		GameData.hud.reset();
		Messager.reset();
		TrackEffects.reset();
		recorder.reset();
		GameData.playerState.reset();
		isFirstLap = true;
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
			CarSoundManager.carImpacted( impulses.len() );
			GameData.driftState.invalidateByCollision();
		}
	}

	@Override
	public void onComputeForces( CarForces forces ) {
		if(recorder.isRecording()) {
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

		if( onStartZone ) {
			if( isFirstLap ) {
				isFirstLap = false;

				lapState.restart();
				Replay buf = lapState.getNextBuffer();
				recorder.beginRecording( player.car, buf, lapState.getStartNanotime() );
				lastRecordedLapId = buf.id;

				if( lapState.hasAnyReplayData() ) {
					Replay any = lapState.getAnyReplay();
					player.ghost.setReplay( any );
				}
			}
			else {
				if( recorder.isRecording() ) recorder.endRecording();

				lapState.update();

				// replay best, overwrite worst logic

				if( !lapState.hasAllReplayData() ) {
					// only one single replay
					lapState.restart();
					Replay buf = lapState.getNextBuffer();
					recorder.beginRecording( player.car, buf, lapState.getStartNanotime() );
					lastRecordedLapId = buf.id;

					Replay any = lapState.getAnyReplay();
					player.ghost.setReplay( any );
					lapState.setLastTrackTimeSeconds( any.trackTimeSeconds );

					Messager.show( "GO!  GO!  GO!", 3f, MessageType.Information, MessagePosition.Middle, MessageSize.Big );
				}
				else {
					// both valid, replay best, overwrite worst
					Replay best = lapState.getBestReplay(), worst = lapState.getWorstReplay();

					if( lastRecordedLapId == best.id ) {
						lapState.setLastTrackTimeSeconds( best.trackTimeSeconds );
						Messager.show( "-" + NumberString.format( worst.trackTimeSeconds - best.trackTimeSeconds ) + " seconds!", 3f, MessageType.Good,
								MessagePosition.Top, MessageSize.Big );
					}
					else {
						lapState.setLastTrackTimeSeconds( worst.trackTimeSeconds );
						Messager.show( "+" + NumberString.format( worst.trackTimeSeconds - best.trackTimeSeconds ) + " seconds", 3f, MessageType.Bad,
								MessagePosition.Top, MessageSize.Big );
					}

					player.ghost.setReplay( best );

					lapState.restart();
					recorder.beginRecording( player.car, worst, lapState.getStartNanotime() );
					lastRecordedLapId = worst.id;
				}
			}
		}
	}


	// ----------------------------------------------------------------------
	//
	// DriftStateListener
	//
	// ----------------------------------------------------------------------

	@Override
	public void onBeginDrift() {
		CarSoundManager.driftBegin();
		// System.out.println("-> drift starts");
	}

	@Override
	public void onEndDrift() {
		CarSoundManager.driftEnd();
		// System.out.println("<- drift ends");
	}
}
