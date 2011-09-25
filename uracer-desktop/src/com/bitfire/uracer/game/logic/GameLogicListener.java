package com.bitfire.uracer.game.logic;

import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.LapInfo;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.entities.vehicles.GhostCar;
import com.bitfire.uracer.hud.Hud;
import com.bitfire.uracer.hud.Messager.MessagePosition;
import com.bitfire.uracer.hud.Messager.MessageSize;
import com.bitfire.uracer.hud.Messager.MessageType;
import com.bitfire.uracer.simulations.car.Replay;
import com.bitfire.uracer.tiled.Level;

public class GameLogicListener implements IGameLogicListener
{
	private GameLogic logic = null;
	private Level level = null;

	private Car player = null;
	private GhostCar ghost = null;

	// lap
	protected LapInfo lapInfo;
	private boolean isFirstLap = true;
	private long lastLapId = 0;

	public GameLogicListener( GameLogic logic )
	{
		this.logic = logic;
		this.level = logic.getGame().getLevel();
		player = logic.getGame().getLevel().getPlayer();
		ghost = logic.getGame().getLevel().getGhost();
		lapInfo = new LapInfo();

		// onBeforeStart?
		Hud.showMessage( "WARM  UP  LAP", 5f, MessageType.Information, MessagePosition.Top, MessageSize.Big );
	}

	@Override
	public void onReset()
	{
		lapInfo.reset();
		isFirstLap = true;
		lastLapId = 0;
	}

	@Override
	public void onRestart()
	{
		isFirstLap = true;

//		Replay r = lapInfo.getFirstAvailable();
//		if(lapInfo.hasAnyReplayData() && r != null )
//		{
//			Replay buf = lapInfo.getNextBuffer();
//			level.beginRecording( buf, lapInfo.getStartNanotime() );
//
//			System.out.println("Replaying next buffer");
//			ghost.setReplay( r );
//			lastLapId = r.id;
//		}
//		else
//		{
//			System.out.println("No replay data");
//			isFirstLap = true;
//			ghost.setReplay( null );
//		}
	}

	@Override
	public LapInfo onGetLapInfo()
	{
		return lapInfo;
	}

	@Override
	public void onTileChanged( Vector2 carAt )
	{
		boolean onStartZone = (carAt.x == 1 && carAt.y == 0);

		if( onStartZone )
		{
//			Replay b0 = lapInfo.getReplay( 0 ), b1 = lapInfo.getReplay( 1 );

			if( isFirstLap )
			{
				isFirstLap = false;

				lapInfo.restart();
				Replay buf = lapInfo.getNextBuffer();
				level.beginRecording( buf, lapInfo.getStartNanotime() );
				lastLapId = buf.id;

				if( lapInfo.hasAnyReplayData() )
					ghost.setReplay( lapInfo.getFirstAvailable() );
			} else
			{
				if(level.isRecording())
					level.endRecording();
				lapInfo.update();

				// replay best, overwrite worst logic

				if( !lapInfo.hasAllReplayData() )
				{
					// only one single replay
					lapInfo.restart();
					Replay buf = lapInfo.getNextBuffer();
					level.beginRecording( buf, lapInfo.getStartNanotime() );
					lastLapId = buf.id;

					ghost.setReplay( lapInfo.getPrevBuffer() );
					Hud.showMessage( "GO!  GO!  GO!", 5f, MessageType.Information, MessagePosition.Top, MessageSize.Big );
				} else
				{
					// both valid, replay best, overwrite worst
					Replay best = lapInfo.getBestReplay(), worst = lapInfo.getWorstReplay();

					if( lastLapId == best.id )
					{
						Hud.showMessage(
								"-" + String.format( "%.2f", worst.trackTimeSeconds - best.trackTimeSeconds )
										+ " seconds!", 5f, MessageType.Good, MessagePosition.Top, MessageSize.Big );
					} else
					{
						Hud.showMessage(
								"+" + String.format( "%.2f", worst.trackTimeSeconds - best.trackTimeSeconds )
										+ " seconds", 5f, MessageType.Bad, MessagePosition.Top, MessageSize.Big );
					}

					ghost.setReplay( best );

					lapInfo.restart();
					level.beginRecording( worst, lapInfo.getStartNanotime() );
					lastLapId = worst.id;
				}
			}
		}
	}

}
