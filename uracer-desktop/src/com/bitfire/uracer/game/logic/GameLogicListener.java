package com.bitfire.uracer.game.logic;

import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.audio.CarSoundManager;
import com.bitfire.uracer.carsimulation.Replay;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.entities.vehicles.GhostCar;
import com.bitfire.uracer.messager.Messager;
import com.bitfire.uracer.messager.Messager.MessagePosition;
import com.bitfire.uracer.messager.Messager.MessageSize;
import com.bitfire.uracer.messager.Messager.MessageType;
import com.bitfire.uracer.tiled.Level;
import com.bitfire.uracer.utils.Convert;
import com.bitfire.uracer.utils.NumberString;

public class GameLogicListener implements IGameLogicListener
{
	private GameLogic logic = null;
	private Level level = null;

	private Car player = null;
	private GhostCar ghost = null;

	// lap
	protected LapInfo lapInfo;
	private boolean isFirstLap = true;
	private long lastRecordedLapId = 0;

	public GameLogicListener( GameLogic logic )
	{
		this.logic = logic;
		this.level = logic.getGame().getLevel();
		this.lapInfo = LapInfo.get();

		this.player = logic.getGame().getLevel().getPlayer();
		this.ghost = logic.getGame().getLevel().getGhost();
	}

	@Override
	public void onCreate()
	{
	}

	@Override
	public void onReset()
	{
		lapInfo.reset();
		isFirstLap = true;
		lastRecordedLapId = 0;
	}

	@Override
	public void onRestart()
	{
		isFirstLap = true;
//		Messager.show( "WARM  UP  LAP", 3f, MessageType.Information, MessagePosition.Middle, MessageSize.Big );


		/**
		 * debug
		 */

//		HudDrifting hud = logic.getGame().getHud().getDrifting();
//		HudLabel label = hud.labelResult;
//		label.setAlpha( 0 );
//		label.setString( "+2.30!" );
//		label.setPosition( Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2);
//		label.setFont( Art.fontCurseYRbig );
//		label.slide( new Vector2(0,-1), 10 );
	}

	@Override
	public void onTileChanged( Vector2 carAt )
	{
		Vector2 cartile = Convert.pxToTile( player.getStartPos().x, player.getStartPos().y );
		boolean onStartZone = (carAt.x == cartile.x && carAt.y == cartile.y);

		if( onStartZone )
		{
			if( isFirstLap )
			{
				isFirstLap = false;

				lapInfo.restart();
				Replay buf = lapInfo.getNextBuffer();
				level.beginRecording( buf, lapInfo.getStartNanotime() );
				lastRecordedLapId = buf.id;

				if( lapInfo.hasAnyReplayData() )
				{
					Replay any = lapInfo.getAnyReplay();
					ghost.setReplay( any );
				}
			} else
			{
				if( level.isRecording() ) level.endRecording();

				lapInfo.update();

				// replay best, overwrite worst logic

				if( !lapInfo.hasAllReplayData() )
				{
					// only one single replay
					lapInfo.restart();
					Replay buf = lapInfo.getNextBuffer();
					level.beginRecording( buf, lapInfo.getStartNanotime() );
					lastRecordedLapId = buf.id;

					Replay any = lapInfo.getAnyReplay();
					ghost.setReplay( any );
					lapInfo.setLastTrackTimeSeconds( any.trackTimeSeconds );

					Messager.show( "GO!  GO!  GO!", 3f, MessageType.Information, MessagePosition.Middle, MessageSize.Big );
				} else
				{
					// both valid, replay best, overwrite worst
					Replay best = lapInfo.getBestReplay(), worst = lapInfo.getWorstReplay();

					if( lastRecordedLapId == best.id )
					{
						lapInfo.setLastTrackTimeSeconds( best.trackTimeSeconds );
						Messager.show( "-" + NumberString.format(worst.trackTimeSeconds - best.trackTimeSeconds)
								+ " seconds!", 3f, MessageType.Good, MessagePosition.Top, MessageSize.Big );
					} else
					{
						lapInfo.setLastTrackTimeSeconds( worst.trackTimeSeconds );
						Messager.show(
								"+" + NumberString.format(worst.trackTimeSeconds - best.trackTimeSeconds) + " seconds", 3f,
								MessageType.Bad, MessagePosition.Top, MessageSize.Big );
					}

					ghost.setReplay( best );

					lapInfo.restart();
					level.beginRecording( worst, lapInfo.getStartNanotime() );
					lastRecordedLapId = worst.id;
				}
			}
		}
	}

	@Override
	public void onBeginDrift()
	{
		logic.getGame().getHud().getDrifting().onBeginDrift();
		CarSoundManager.driftBegin();
//		System.out.println("-> drift starts");
	}

	@Override
	public void onEndDrift()
	{
		logic.getGame().getHud().getDrifting().onEndDrift();
		CarSoundManager.driftEnd();
//		System.out.println("<- drift ends");
	}
}
