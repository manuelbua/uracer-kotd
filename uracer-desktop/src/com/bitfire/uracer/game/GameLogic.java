
package com.bitfire.uracer.game;

import com.bitfire.uracer.game.actors.CarPreset;

public interface GameLogic {
	void setPlayer (CarPreset.Type presetType);

	void removePlayer ();

	void restartGame ();

	void resetGame ();

	void quitGame ();

	void startTimeDilation ();

	void endTimeDilation ();

	boolean timeDilationAvailable ();

	void dispose ();

	void tick ();

	void tickCompleted ();

	void beforeRender ();
}
