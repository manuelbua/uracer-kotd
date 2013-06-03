
package com.bitfire.uracer.game;

import com.bitfire.uracer.game.actors.CarPreset;

public interface GameLogic {
	void dispose ();

	void setPlayer (CarPreset.Type presetType);

	void removePlayer ();

	void restartGame ();

	void resetGame ();

	void quitGame ();

	void startTimeDilation ();

	void endTimeDilation ();

	boolean isTimeDilationAvailable ();

	void tick ();

	void tickCompleted ();

	void beforeRender ();
}
