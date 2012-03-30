package com.bitfire.uracer.game;

import com.bitfire.uracer.audio.CarSoundManager;
import com.bitfire.uracer.carsimulation.Replay;
import com.bitfire.uracer.game.logic.IGameLogicListener;
import com.bitfire.uracer.game.logic.LapState;
import com.bitfire.uracer.game.logic.Level;
import com.bitfire.uracer.game.logic.PlayerState;
import com.bitfire.uracer.messager.Messager;
import com.bitfire.uracer.messager.Messager.MessagePosition;
import com.bitfire.uracer.messager.Messager.MessageSize;
import com.bitfire.uracer.messager.Messager.MessageType;
import com.bitfire.uracer.utils.NumberString;

public class GameLogicListener implements IGameLogicListener {
	private Level level = null;

	// lap
	private boolean isFirstLap = true;
	private long lastRecordedLapId = 0;

	public GameLogicListener( Level level ) {
		this.level = level;
	}

	@Override
	public void onCreate() {
	}

	@Override
	public void onReset() {
		GameData.lapState.reset();
		isFirstLap = true;
		lastRecordedLapId = 0;
	}

	@Override
	public void onRestart() {
		isFirstLap = true;
		// Messager.show( "WARM  UP  LAP", 3f, MessageType.Information, MessagePosition.Middle, MessageSize.Big );

		/** debug */

		// HudDrifting hud = logic.getGame().getHud().getDrifting();
		// HudLabel label = hud.labelResult;
		// label.setAlpha( 0 );
		// label.setString( "+2.30!" );
		// label.setPosition( Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2);
		// label.setFont( Art.fontCurseYRbig );
		// label.slide( new Vector2(0,-1), 10 );
	}

	@Override
	public void onTileChanged( PlayerState player ) {
		boolean onStartZone = (player.currTileX == player.startTileX && player.currTileY == player.startTileY);

		LapState lapState = GameData.lapState;

		if( onStartZone ) {
			if( isFirstLap ) {
				isFirstLap = false;

				lapState.restart();
				Replay buf = lapState.getNextBuffer();
				level.beginRecording( buf, lapState.getStartNanotime() );
				lastRecordedLapId = buf.id;

				if( lapState.hasAnyReplayData() ) {
					Replay any = lapState.getAnyReplay();
					player.ghost.setReplay( any );
				}
			}
			else {
				if( level.isRecording() ) level.endRecording();

				lapState.update();

				// replay best, overwrite worst logic

				if( !lapState.hasAllReplayData() ) {
					// only one single replay
					lapState.restart();
					Replay buf = lapState.getNextBuffer();
					level.beginRecording( buf, lapState.getStartNanotime() );
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
					level.beginRecording( worst, lapState.getStartNanotime() );
					lastRecordedLapId = worst.id;
				}
			}
		}
	}

	@Override
	public void onBeginDrift() {
		GameData.hud.getDrifting().onBeginDrift();
		CarSoundManager.driftBegin();
		// System.out.println("-> drift starts");
	}

	@Override
	public void onEndDrift() {
		GameData.hud.getDrifting().onEndDrift();
		CarSoundManager.driftEnd();
		// System.out.println("<- drift ends");
	}
}
