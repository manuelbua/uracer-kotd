package com.bitfire.uracer.game.logic;

import com.badlogic.gdx.math.Vector2;
import com.bitfire.uracer.LapInfo;

public interface IGameLogicListener
{
	public abstract void onReset();
	public abstract void onRestart();
	public abstract LapInfo onGetLapInfo();
	public abstract void onTileChanged( Vector2 carAt );
	public abstract void onBeginDrift();
	public abstract void onEndDrift();
}
