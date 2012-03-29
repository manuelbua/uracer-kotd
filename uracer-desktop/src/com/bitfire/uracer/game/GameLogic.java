package com.bitfire.uracer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.game.logic.DriftState;
import com.bitfire.uracer.game.logic.IGameLogicListener;
import com.bitfire.uracer.game.logic.LapState;
import com.bitfire.uracer.game.logic.Level;

public class GameLogic {
	protected Game game;
	protected IGameLogicListener listener;

	// lap and entities
	private Level level;

	public GameLogic( Game game, LapState lapState ) {
		this.game = game;
		this.level = game.getLevel();

		DriftState.init( this );

		this.listener = new GameLogicListener( this, lapState );
	}

	public void create() {
		reset();
		listener.onCreate();
	}

	public void dispose() {
	}

	public boolean tick() {
		EntityManager.raiseOnTick(game.world);

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
		DriftState.get().update( level.getPlayer().car );

		return true;
	}

	public void reset() {
		restart();
		listener.onReset();
	}

	public void restart() {
		Game.getTweener().clear();

		// reset drift info and hud drifting component
		DriftState.get().reset();
		if( game.getHud() != null ) game.getHud().getDrifting().reset();

		listener.onRestart();
	}

	public IGameLogicListener getListener() {
		return listener;
	}
}
