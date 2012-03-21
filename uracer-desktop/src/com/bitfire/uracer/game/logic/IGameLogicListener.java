package com.bitfire.uracer.game.logic;

public interface IGameLogicListener {
	public abstract void onCreate();

	public abstract void onReset();

	public abstract void onRestart();

	public abstract void onTileChanged( Player player );

	public abstract void onBeginDrift();

	public abstract void onEndDrift();
}
