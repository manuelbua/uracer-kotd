package com.bitfire.uracer.game.logic;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.bitfire.uracer.Config;
import com.bitfire.uracer.Director;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.effects.postprocessing.PostProcessor;
import com.bitfire.uracer.effects.postprocessing.RadialBlur;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.game.Game;
import com.bitfire.uracer.tiled.Level;

public class GameLogic
{
	private Game game;
	private IGameLogicListener listener;

	// events - onTileChanged

	// lap and entities
	private Level level;

	// effects
	private RadialBlur rb;

	public GameLogic( Game game )
	{
		this.game = game;
		this.level = game.getLevel();

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

		level.getPlayer().update(listener);

		if( Config.Graphics.EnablePostProcessingFx )
		{
			rb.dampStrength( 0.8f );
			rb.setOrigin( Director.screenPosFor( level.getPlayer().car.getBody() ) );
		}

		// update DriftInfo, handle raising onBeginDrift / onEndDrift
		DriftInfo.get().update( level.getPlayer().car );
	}

	public void reset()
	{
		restart();
		listener.onReset();
	}

	public void restart()
	{
		Game.getTweener().clear();

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
