
package com.bitfire.uracer.game.logic.post;

import com.badlogic.gdx.graphics.Color;
import com.bitfire.uracer.game.player.PlayerCar;

public interface PostProcessingAnimator {
	void update (Color ambient, Color trees, float zoom, float warmUpCompletion, float collisionFactor);

	void alertBegins (int milliseconds);

	void alertEnds (int milliseconds);

	public void alert (int milliseconds);

	void reset ();

	void setPlayer (PlayerCar player);
}
