
package com.bitfire.uracer.game;

public interface GameLogic {
	void dispose ();

	void addPlayer ();

	void removePlayer ();

	void restartGame ();

	void resetGame ();

	void quitGame ();

	boolean isQuitPending ();

	float getCollisionFactor ();

	void startTimeDilation ();

	void endTimeDilation ();

	boolean isTimeDilationAvailable ();

	void tick ();

	void tickCompleted ();
}
