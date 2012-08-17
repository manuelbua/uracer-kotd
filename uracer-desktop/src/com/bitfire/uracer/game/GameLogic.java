
package com.bitfire.uracer.game;

import com.bitfire.uracer.game.actors.CarPreset;
import com.bitfire.uracer.game.logic.replaying.Replay;

public interface GameLogic {
	void setPlayer (CarPreset.Type presetType);

	void dispose ();

	void tick ();

	void tickCompleted ();

	void beforeRender ();

	// from player
	void newReplay (Replay replay);
}
