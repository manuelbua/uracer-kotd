package com.bitfire.uracer.game.audio;

public interface ISoundEffect {
	void onDispose();

	void onStart();

	void onTick();

	void onStop();

	void onReset();
}
