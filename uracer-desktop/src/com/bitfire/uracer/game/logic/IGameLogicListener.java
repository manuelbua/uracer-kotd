package com.bitfire.uracer.game.logic;

import com.badlogic.gdx.math.Vector2;

public interface IGameLogicListener
{
	public abstract void onReset();
	public abstract void onRestart();
	public abstract LapInfo onGetLapInfo();
	public abstract void onTileChanged( Vector2 carAt );
	public abstract void onBeginDrift(Vector2 trackedDrift);
	public abstract void onEndDrift();
	public abstract boolean isDrifting();
	public abstract DriftInfo onGetDriftInfo();
}
