package com.bitfire.uracer.game.logic;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.effects.postprocessing.PostProcessor;
import com.bitfire.uracer.effects.postprocessing.RadialBlur;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.entities.vehicles.Car;
import com.bitfire.uracer.game.Game;
import com.bitfire.uracer.tiled.Level;
import com.bitfire.uracer.utils.Convert;

public class GameLogic
{
	private Game game;
	private IGameLogicListener listener;

	// events - onTileChanged
	private Vector2 carTileAt = new Vector2();
	private Vector2 lastCarTileAt = new Vector2();

	// lap and entities
	private Level level;
	private Car player = null;

	// effects
	private RadialBlur rb;

	public GameLogic( Game game )
	{
		this.game = game;
		this.level = game.getLevel();
		this.player = level.getPlayer();

		// effects
		if( Config.Graphics.EnablePostProcessingFx )
		{
			rb = new RadialBlur();
			rb.setEnabled( true );
			PostProcessor.init( Gdx.graphics.getWidth(), Gdx.graphics.getHeight() );
//			 PostProcessor.init( 512, 512 );
			PostProcessor.setEffect( rb );
		}

		DriftInfo.init( this );
		LapInfo.init();

		this.listener = new GameLogicListener( this );
	}

	public void create()
	{
		reset();
		listener.onCreate();
	}

	public void dispose()
	{
	}

	public void tick()
	{
		EntityManager.raiseOnTick();

		if( player != null )
		{
			if( Input.isOn( Keys.R ) )
			{
				game.restart();
			} else if( Input.isOn( Keys.T ) )
			{
				game.reset();
			} else if( Input.isOn( Keys.Q ) )
			{
				Gdx.app.exit();
			}

			// onTileChanged
			lastCarTileAt.set( carTileAt );
			carTileAt.set( Convert.pxToTile( player.pos().x, player.pos().y ) );
			if( (lastCarTileAt.x != carTileAt.x) || (lastCarTileAt.y != carTileAt.y) )
			{
				listener.onTileChanged( carTileAt );
			}

			if( Config.Graphics.EnablePostProcessingFx )
			{
				rb.dampStrength( 0.8f );
				rb.setOrigin( Director.screenPosFor( player.getBody() ) );
			}

			// update DriftInfo, handle raising onBeginDrift / onEndDrift
			DriftInfo.get().update( player );
		}
	}

	public void reset()
	{
		restart();
		listener.onReset();
	}

	public void restart()
	{
		Game.getTweener().clear();

		// causes an onTileChanged event to be raised
		lastCarTileAt.set( -1, -1 );
		carTileAt.set( lastCarTileAt );

		// reset drift info and hud drifting component
		DriftInfo.get().reset();
		if(game.getHud()!=null) game.getHud().getDrifting().reset();

		listener.onRestart();
	}

	public Game getGame()
	{
		return game;
	}

	public IGameLogicListener getListener()
	{
		return listener;
	}
}
