package com.bitfire.uracer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.entities.vehicles.GhostCar;
import com.bitfire.uracer.hud.Hud;
import com.bitfire.uracer.hud.Messager.MessagePosition;
import com.bitfire.uracer.hud.Messager.MessageType;
import com.bitfire.uracer.postprocessing.PostProcessor;
import com.bitfire.uracer.postprocessing.effects.RadialBlur;
import com.bitfire.uracer.simulations.car.Replay;
import com.bitfire.uracer.tiled.Level;
import com.bitfire.uracer.utils.Convert;

public class GameLogic
{
	private Game game;

	// events - onTileChanged
	private boolean isFirstLap = true;
	private long lastLapId = 0;
	private Vector2 carTileAt = new Vector2();
	private Vector2 lastCarTileAt = new Vector2();

	// lap and entities
	private Level level;
	private LapInfo lapInfo;
	private Car player = null;
	private GhostCar ghost = null;

	// effects
	private RadialBlur rb;

	public GameLogic( Game game )
	{
		this.game = game;
		this.level = game.getLevel();

		// get player/ghost references
		this.player = level.getPlayer();
		this.ghost = level.getGhost();

		// lap info
		lapInfo = new LapInfo();

		// effects
		if( Config.EnablePostProcessingFx )
		{
			rb = new RadialBlur();
			rb.setEnabled( true );
			PostProcessor.init( Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );
			// PostProcessor.init( 512, 512 );
			PostProcessor.setEffect( rb );
		}

		reset();
	}

	public void tick()
	{
		if( Input.isOn( Keys.R ) )
		{
			if( player != null )
			{
				game.restart();
			}
		}

		if( player != null )
		{
			lastCarTileAt.set( carTileAt );
			carTileAt.set( Convert.pxToTile( player.pos().x, player.pos().y ) );
			if( (lastCarTileAt.x != carTileAt.x) || (lastCarTileAt.y != carTileAt.y) )
			{
				onTileChanged( carTileAt );
			}

			if( Config.EnablePostProcessingFx )
			{
				rb.dampStrength( 0.8f, Physics.dt );
				rb.setOrigin( Director.screenPosFor( player.getBody() ) );
			}
		}
	}

	public void reset()
	{
		lapInfo.reset();
		restart();
		isFirstLap = true;
	}

	public void restart()
	{
		// restart the level, do NOT reset lap info
		lastLapId = 0;

		lastCarTileAt.set( -1, -1 );
		carTileAt.set( lastCarTileAt );
		lapInfo.restart();
	}

	protected void onTileChanged( Vector2 carAt )
	{
		boolean onStartZone = (carAt.x == 1 && carAt.y == 0);
		if( onStartZone )
		{
			Replay b0 = lapInfo.getReplay( 0 ), b1 = lapInfo.getReplay( 1 );

			if( isFirstLap )
			{
				isFirstLap = false;

				level.beginRecording( b0, lapInfo.restart() );
				lapInfo.setAsLast( null );
				lastLapId = b0.id;

				Hud.showMessage( "WARM  UP  LAP", 5f, MessageType.Information, MessagePosition.Top );
			} else
			{
				level.endRecording();
				lapInfo.update();

				// replay best, overwrite worst logic

				if( !lapInfo.hasReplayData() )
				{
					// only one single replay
					level.beginRecording( b1, lapInfo.restart() );
					lapInfo.setAsLast( b0 );
					lastLapId = b1.id;

					ghost.setReplay( b0 );
					Hud.showMessage( "GO!  GO!  GO!", 5f, MessageType.Information, MessagePosition.Top );
				} else
				{
					// both valid, replay best, overwrite worst
					Replay best = lapInfo.getBestReplay(), worst = lapInfo.getWorstReplay();

					if( lastLapId == best.id )
					{
						Hud.showMessage(
								"-" + String.format( "%.2f", worst.trackTimeSeconds - best.trackTimeSeconds )
										+ "\nseconds!", 5f, MessageType.Good, MessagePosition.Top );
					} else
					{
						Hud.showMessage(
								"+" + String.format( "%.2f", worst.trackTimeSeconds - best.trackTimeSeconds )
										+ "\nseconds", 5f, MessageType.Bad, MessagePosition.Top );
					}

					ghost.setReplay( best );

					level.beginRecording( worst, lapInfo.restart() );
					lastLapId = worst.id;
				}
			}
		}
	}

	public LapInfo getLapInfo()
	{
		return lapInfo;
	}
}
