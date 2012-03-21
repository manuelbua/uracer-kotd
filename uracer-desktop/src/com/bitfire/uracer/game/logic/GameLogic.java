package com.bitfire.uracer.game.logic;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.game.Game;

public class GameLogic {
	private Game game;
	private IGameLogicListener listener;

	// lap and entities
	private Level level;

	public GameLogic( Game game ) {
		this.game = game;
		this.level = game.getLevel();

		DriftInfo.init( this );
		LapInfo.init();

		this.listener = new GameLogicListener( this );
	}

	public void create() {
		reset();
		listener.onCreate();
	}

	public void dispose() {
	}

	public boolean tick() {
		EntityManager.raiseOnTick();

		if( Input.isOn( Keys.R ) ) {
			game.restart();
		}
		else if( Input.isOn( Keys.T ) ) {
			game.reset();
		}
		else if( Input.isOn( Keys.Q ) ) {
			Gdx.app.exit();
			return false;
		}

		level.getPlayer().update( listener );

		// update DriftInfo, handle raising onBeginDrift / onEndDrift
		DriftInfo.get().update( level.getPlayer().car );

		return true;
	}

	public void reset() {
		restart();
		listener.onReset();
	}

	public void restart() {
		Game.getTweener().clear();

		// reset drift info and hud drifting component
		DriftInfo.get().reset();
		if( game.getHud() != null ) game.getHud().getDrifting().reset();

		listener.onRestart();
	}

	public Game getGame() {
		return game;
	}

	public IGameLogicListener getListener() {
		return listener;
	}
}
