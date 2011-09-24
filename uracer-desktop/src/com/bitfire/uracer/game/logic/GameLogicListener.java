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
	private boolean isFirstLap = true;
	private long lastLapId = 0;

	public GameLogicListener( GameLogic logic )
	{
		this.logic = logic;
		this.level = logic.getGame().getLevel();
		player = logic.getGame().getLevel().getPlayer();
		ghost = logic.getGame().getLevel().getGhost();
	}

	@Override
	public void onReset()
	{
		isFirstLap = true;
	}

	@Override
	public void onRestart()
	{
		lastLapId = 0;
		if(!logic.getLapInfo().hasAnyReplayData())
		{
			System.out.println("no replay data, re-firstlap");
			isFirstLap = true;
		}
	}

	@Override
	public void onTileChanged( Vector2 carAt )
	{
		boolean onStartZone = (carAt.x == 1 && carAt.y == 0);
		LapInfo lapInfo = logic.getLapInfo();

		if( onStartZone )
		{
			Replay b0 = lapInfo.getReplay( 0 ), b1 = lapInfo.getReplay( 1 );

			if( isFirstLap )
			{
				isFirstLap = false;

				level.beginRecording( b0, lapInfo.restart() );
				lapInfo.setAsLast( null );
				lastLapId = b0.id;

				Hud.showMessage( "WARM  UP  LAP", 5f, MessageType.Information, MessagePosition.Top, MessageSize.Big );
			} else
			{
				level.endRecording();
				lapInfo.update();

				// replay best, overwrite worst logic

				if( !lapInfo.hasAllReplayData() )
				{
					// only one single replay
					level.beginRecording( b1, lapInfo.restart() );
					lapInfo.setAsLast( b0 );
					lastLapId = b1.id;

					ghost.setReplay( b0 );
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

					level.beginRecording( worst, lapInfo.restart() );
					lastLapId = worst.id;
				}
			}
		}
	}

}
