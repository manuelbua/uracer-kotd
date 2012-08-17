
package com.bitfire.uracer.game;


public interface GameLogic {
	void dispose ();

	void tick ();

	void tickCompleted ();

	void beforeRender ();
}
