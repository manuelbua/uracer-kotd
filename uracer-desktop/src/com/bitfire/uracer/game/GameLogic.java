package com.bitfire.uracer.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.bitfire.uracer.Input;
import com.bitfire.uracer.entities.EntityManager;
import com.bitfire.uracer.game.logic.IGameLogicListener;

public class GameLogic {
	protected Game game;

	// lap and entities
	private IGameLogicListener listener;

	// the convention here is that it is safe to pass the "this" pointer
	// as a constructor parameter from the "this" constructor method itself,
	// since the constructor NEVER EVER *dereference* the "this" parameter
	// in ITS constructor method.		// ttp
	public GameLogic( Game game, IGameLogicListener listener ) {
		this.game = game;
		this.listener = listener;
	}

	public boolean tick() {
		EntityManager.raiseOnTick(GameData.world);

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

		GameData.playerState.update( listener );
		GameData.driftState.update();

		return true;
	}

	public void reset() {
		restart();
		listener.onReset();
	}

	public void restart() {
		GameData.tweener.clear();

		// reset drift info and hud drifting component
		GameData.driftState.reset();
		GameData.hud.getDrifting().reset();

		listener.onRestart();
	}

	public IGameLogicListener getListener() {
		return listener;
	}
}
