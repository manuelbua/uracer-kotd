package com.bitfire.uracer.game.logic;

import com.badlogic.gdx.math.Vector2;

public interface IGameLogicListener
{
	public abstract void onReset();
	public abstract void onRestart();
	public abstract void onTileChanged( Vector2 carAt );
}
